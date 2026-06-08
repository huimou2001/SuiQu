package com.suiqu.cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.suiqu.cloud.entity.FileInfo;
import com.suiqu.cloud.entity.FileUser;
import com.suiqu.cloud.entity.vo.CheckUploadVO;
import com.suiqu.cloud.entity.vo.FileVO;
import com.suiqu.cloud.mapper.FileInfoMapper;
import com.suiqu.cloud.mapper.FileUserMapper;
import com.suiqu.cloud.service.FileService;
import com.suiqu.cloud.utils.SecurityUtils;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Autowired private MinioClient minioClient;
    @Autowired private FileInfoMapper fileInfoMapper; // 物理表
    @Autowired private FileUserMapper fileUserMapper; // 逻辑表
    @Autowired private RedisTemplate<String, Object> redisTemplate;

    private static final String BUCKET_NAME = "suiqu-files";
    private static final String TEMP_BUCKET = "temp-chunks";

    @Override
    public CheckUploadVO checkFile(String md5) {
        // 1. 秒传检查：去物理文件表查询 MD5
        FileInfo physicalFile = fileInfoMapper.selectByMd5(md5);
        if (physicalFile != null) {
            return new CheckUploadVO(true, null);
        }

        // 2. 断点续传检查
        Set<Object> uploadedChunks = redisTemplate.opsForSet().members("upload:chunks:" + md5);
        List<Integer> chunkList = uploadedChunks == null ? new ArrayList<>() :
                uploadedChunks.stream().map(o -> (Integer)o).collect(Collectors.toList());

        return new CheckUploadVO(false, chunkList);
    }

    @Override
    public void uploadChunk(MultipartFile file, String md5, Integer index) {
        String objectName = md5 + "/" + index;
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(TEMP_BUCKET).object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType("application/octet-stream").build());

            redisTemplate.opsForSet().add("upload:chunks:" + md5, index);
            redisTemplate.expire("upload:chunks:" + md5, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("分片上传失败: {}", e.getMessage());
            throw new RuntimeException("分片上传失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void mergeChunks(String md5, String fileName, Integer totalChunks, Long parentId) {
        try {
            Long userId = SecurityUtils.getUserId();
            FileInfo physicalFile = fileInfoMapper.selectByMd5(md5);

            // 1. 处理物理文件记录 (FileInfo)
            if (physicalFile == null) {
                // 物理文件不存在，执行 MinIO 合并
                String uuidName = UUID.randomUUID().toString().replace("-", "");
                String targetPath = "data/" + uuidName;

                List<ComposeSource> sources = new ArrayList<>();
                for (int i = 0; i < totalChunks; i++) {
                    sources.add(ComposeSource.builder().bucket(TEMP_BUCKET).object(md5 + "/" + i).build());
                }

                minioClient.composeObject(ComposeObjectArgs.builder()
                        .bucket(BUCKET_NAME).object(targetPath).sources(sources).build());

                // 获取合并后的真实大小
                StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder().bucket(BUCKET_NAME).object(targetPath).build());

                physicalFile = new FileInfo();
                physicalFile.setFileUuidName(uuidName);
                physicalFile.setMd5(md5);
                physicalFile.setPath(targetPath);
                physicalFile.setUserCount(1);
                physicalFile.setType(getFileSuffix(fileName));
                physicalFile.setSize(stat.size());
                fileInfoMapper.insert(physicalFile);
            } else {
                // 秒传逻辑：物理表引用计数原子 +1
                fileInfoMapper.incrementUserCount(physicalFile.getId());
            }

            // 2. 处理用户逻辑记录 (FileUser)
            FileUser fileUser = new FileUser();
            fileUser.setUserId(userId);
            fileUser.setFileName(fileName);
            fileUser.setParentId(parentId);
            fileUser.setFileId(physicalFile.getId());
            fileUser.setDescription("");
            fileUser.setIsDir(0);
            fileUser.setCreateTime(LocalDateTime.now());
            fileUserMapper.insert(fileUser);

            // 3. 异步清理临时分片
            CompletableFuture.runAsync(() -> {
                for (int i = 0; i < totalChunks; i++) {
                    try {
                        minioClient.removeObject(RemoveObjectArgs.builder().bucket(TEMP_BUCKET).object(md5 + "/" + i).build());
                    } catch (Exception ignored) {}
                }
                redisTemplate.delete("upload:chunks:" + md5);
            });

        } catch (Exception e) {
            log.error("文件合并失败: {}", e.getMessage());
            throw new RuntimeException("合并文件失败");
        }
    }


    @Override
    public void updateFileDescription(Long userFileId, String description) {
        Long userId = SecurityUtils.getUserId();
        // 1. 校验文件所有权
        FileUser userFile = fileUserMapper.selectById(userFileId);
        if (userFile == null || !userFile.getUserId().equals(userId)) {
            throw new RuntimeException("文件不存在或无权操作");
        }

        // 2. 更新描述
        FileUser updateEntity = new FileUser();
        updateEntity.setId(userFileId);
        updateEntity.setDescription(description);
        fileUserMapper.updateById(updateEntity);

        // 注意：如果是 Canal 同步 ES，这里 update 后，Canal 会自动捕捉并更新 ES 里的描述
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long userFileId, Long userId) {
        // 1. 查询逻辑文件是否存在
        FileUser userFile = fileUserMapper.selectById(userFileId);
        if (userFile == null || !userFile.getUserId().equals(userId)) {
            throw new RuntimeException("文件不存在或无权访问");
        }

        // 2. 如果是文件，处理物理引用计数
        if (userFile.getIsDir() == 0) {
            Long physicalId = userFile.getFileId();
            fileInfoMapper.decrementUserCount(physicalId); // 原子 -1

            FileInfo physicalFile = fileInfoMapper.selectById(physicalId);
            // 3. 如果引用计数归零，彻底删除物理文件
            if (physicalFile != null && physicalFile.getUserCount() <= 0) {
                try {
                    minioClient.removeObject(RemoveObjectArgs.builder()
                            .bucket(BUCKET_NAME).object(physicalFile.getPath()).build());
                    fileInfoMapper.deleteById(physicalId);
                } catch (Exception e) {
                    log.error("MinIO物理删除失败: {}", e.getMessage());
                }
            }
        }
        // 4. 删除逻辑记录
        fileUserMapper.deleteById(userFileId);
    }

    @Override
    public List<FileVO> getUserFiles(Long userId, Long parentId) {
        return fileUserMapper.selectFileList(userId, parentId == null ? 0L : parentId);
    }

    @Override
    public void createDirectory(String name, Long parentId) {
        FileUser dir = new FileUser();
        dir.setFileName(name);
        dir.setUserId(SecurityUtils.getUserId());
        dir.setParentId(parentId == null ? 0L : parentId);
        dir.setIsDir(1);
        dir.setCreateTime(LocalDateTime.now());
        fileUserMapper.insert(dir);
    }

    private String getFileSuffix(String fileName) {
        return (fileName != null && fileName.contains(".")) ?
                fileName.substring(fileName.lastIndexOf(".") + 1) : "";
    }
}