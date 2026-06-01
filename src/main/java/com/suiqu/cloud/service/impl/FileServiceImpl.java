package com.suiqu.cloud.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.suiqu.cloud.entity.FileInfo;
import com.suiqu.cloud.entity.vo.CheckUploadVO;
import com.suiqu.cloud.mapper.FileMapper;
import com.suiqu.cloud.service.FileService;
import com.suiqu.cloud.utils.SecurityUtils;
import io.minio.*;
import io.minio.ComposeSource;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FileServiceImpl implements FileService {

    @Autowired
    private MinioClient minioClient;
    @Autowired
    private FileMapper fileMapper;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String BUCKET_NAME = "suiqu-files";
    private static final String TEMP_BUCKET = "temp-chunks";

    @Override
    public CheckUploadVO checkFile(String md5) {
        // 1. 秒传检查：查询数据库是否存在该MD5的文件
        FileInfo existFile = fileMapper.selectOne(new LambdaQueryWrapper<FileInfo>().eq(FileInfo::getMd5, md5).last("limit 1"));
        if (existFile != null) {
            return new CheckUploadVO(true, null); // 秒传成功
        }

        // 2. 断点续传检查：从Redis获取已上传的分片索引集合
        Set<Object> uploadedChunks = redisTemplate.opsForSet().members("upload:chunks:" + md5);
        List<Integer> chunkList = uploadedChunks.stream().map(o -> (Integer)o).collect(Collectors.toList());

        return new CheckUploadVO(false, chunkList);
    }

    @Override
    public void uploadChunk(MultipartFile file, String md5, Integer index) {
        String objectName = md5 + "/" + index;
        try {
            // 上传分片到临时桶
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(TEMP_BUCKET)
                    .object(objectName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType("application/octet-stream")
                    .build());

            // 记录到Redis
            redisTemplate.opsForSet().add("upload:chunks:" + md5, index);
            redisTemplate.expire("upload:chunks:" + md5, 24, TimeUnit.HOURS);
        } catch (Exception e) {
            log.error("分片上传失败: {}", e.getMessage());
            throw new RuntimeException("分片上传失败");
        }
    }

    @Override
    @Transactional
    public void mergeChunks(String md5, String fileName, Integer totalChunks, Long parentId) {
        try {
            // 1. 构造合并源列表 (ComposeSource)
            List<ComposeSource> sources = new ArrayList<>();
            for (int i = 0; i < totalChunks; i++) {
                sources.add(ComposeSource.builder()
                        .bucket(TEMP_BUCKET)
                        .object(md5 + "/" + i) // 分片的临时路径
                        .build());
            }

            // 2. 构造合并请求参数 (使用 ComposeObjectArgs 代替 ComposeOptions)
            String targetObjectName = md5 + "/" + fileName; // 合并后的正式路径

            ComposeObjectArgs composeArgs = ComposeObjectArgs.builder()
                    .bucket(BUCKET_NAME)    // 目标桶
                    .object(targetObjectName) // 目标文件名
                    .sources(sources)        // 源分片列表
                    .build();

            // 3. 执行合并
            minioClient.composeObject(composeArgs);

            // 4. 保存到数据库逻辑 (保持不变)
            FileInfo fileInfo = new FileInfo();
            fileInfo.setName(fileName);
            fileInfo.setMd5(md5);
            fileInfo.setSize(minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(BUCKET_NAME)
                            .object(targetObjectName) // 合成后的目标文件名
                            .build()
            ).size());//字节数
            fileInfo.setType(getFileSuffix(fileName));
            fileInfo.setPath(targetObjectName);
            fileInfo.setUserId(SecurityUtils.getUserId());
            fileInfo.setParentId(parentId);
            fileInfo.setIsDir(0);
            fileInfo.setCreateTime(LocalDateTime.now());
            fileMapper.insert(fileInfo);

            // 5. 清理：异步删除临时分片并清除Redis
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
            // 5. 清理：异步删除临时分片并清除Redis
            CompletableFuture.runAsync(() -> {
                for (int i = 0; i < totalChunks; i++) {
                    try {
                        minioClient.removeObject(RemoveObjectArgs.builder().bucket(TEMP_BUCKET).object(md5 + "/" + i).build());
                    } catch (Exception ignored) {}
                }
                redisTemplate.delete("upload:chunks:" + md5);
            });
            throw new RuntimeException("合并文件失败");
        }
    }

    private String getFileSuffix(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ""; // 无后缀
        }
        // 从最后一个 . 开始截取
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    @Override
    public List<FileInfo> getUserFiles(Long userId, Long parentId) {
        // 调用之前我们在 FileMapper 中定义的 selectByParentId 方法
        // 如果 parentId 为空，可以默认设为 0（根目录）
        if (parentId == null) {
            parentId = 0L;
        }

        // 这里也可以使用 MyBatis-Plus 的 LambdaQuery 灵活实现
        return fileMapper.selectList(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getParentId, parentId)
                // 排序逻辑：文件夹在前，按创建时间倒叙
                .orderByDesc(FileInfo::getIsDir)
                .orderByDesc(FileInfo::getCreateTime));
    }

    // FileServiceImpl.java
    public void createDirectory(String name, Long parentId) {
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(name);
        fileInfo.setUserId(SecurityUtils.getUserId());
        fileInfo.setParentId(parentId);
        fileInfo.setIsDir(1); // 标记为文件夹
        fileInfo.setCreateTime(LocalDateTime.now());
        fileMapper.insert(fileInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFile(Long fileId, Long userId) {
        // 1. 查询文件是否存在
        FileInfo fileInfo = fileMapper.selectById(fileId);
        if (fileInfo == null) {
            throw new RuntimeException("文件不存在");
        }

        // 2. 权限校验：只能删除自己的文件
        if (!fileInfo.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此文件");
        }

        // 3. 处理文件夹逻辑 (如果是文件夹，不涉及MinIO物理删除，仅递归删库)
        if (fileInfo.getIsDir() == 1) {
            // 这里可以实现递归删除文件夹下所有记录的逻辑
            fileMapper.deleteById(fileId);
            return;
        }

        // 4. 处理文件逻辑：引用计数检查
        String md5 = fileInfo.getMd5();

        // 查询数据库中还有多少条记录指向这个 MD5
        Long count = fileMapper.selectCount(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getMd5, md5));

        // 5. 执行数据库删除
        fileMapper.deleteById(fileId);

        // 6. 如果 count == 1，说明该用户是该物理文件的最后一个持有者，执行 MinIO 物理删除
        if (count == 1) {
            try {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(BUCKET_NAME)
                        .object(fileInfo.getPath()) // 文件在MinIO中的路径
                        .build());
                log.info("物理文件删除成功: {}", fileInfo.getPath());
            } catch (Exception e) {
                log.error("MinIO物理删除失败: {}", e.getMessage());
                // 注意：这里可以选择抛异常回滚，也可以记录日志后续处理
                throw new RuntimeException("物理文件清除失败");
            }
        } else {
            log.info("仍有 {} 个用户持有该 MD5，仅执行逻辑删除", count - 1);
        }

        // 7. 同步删除 ES 中的索引（可选，如果配置了 Canal，Canal 会自动处理此步）
        // 如果没配 Canal，需要在这里手动调 esOperations.delete
    }


    @Async("uploadExecutor") // 使用上面定义的线程池
    public void someHeavyProcessAfterUpload(String md5) {
        // 比如：上传后异步分析文件类型、生成缩略图等
        log.info("异步处理线程: {}", Thread.currentThread().getName());
    }
}
