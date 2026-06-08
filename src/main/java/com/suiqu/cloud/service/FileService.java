package com.suiqu.cloud.service;

import com.suiqu.cloud.entity.FileInfo;
import com.suiqu.cloud.entity.vo.CheckUploadVO;
import com.suiqu.cloud.entity.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {
    // 检查文件状态（秒传及断点续传检查）
    CheckUploadVO checkFile(String md5);

    // 上传分片
    void uploadChunk(MultipartFile file, String md5, Integer index);

    // 合并分片
    public void mergeChunks(String md5, String fileName, Integer totalChunks, Long parentId);


    void updateFileDescription(Long userFileId, String description);
    /**
     * 获取用户指定目录下的文件列表
     * @param userId 用户ID
     * @param parentId 父文件夹ID（根目录为0）
     * @return 文件列表
     */
    public List<FileVO> getUserFiles(Long userId, Long parentId);

    void createDirectory(String name, Long parentId);

    void deleteFile(Long id, Long userId);
}
