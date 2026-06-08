package com.suiqu.cloud.controller;

import com.suiqu.cloud.entity.Result;
import com.suiqu.cloud.entity.dto.FileMergeRequest;
import com.suiqu.cloud.service.FileService;
import com.suiqu.cloud.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/file")
public class FileController {
    @Autowired
    private FileService fileService;

    /**
     * 秒传与断点续传检查
     * @param md5 文件的唯一标识
     */
    @GetMapping("/check")
    public Result checkFile(@RequestParam String md5) {
        // 返回结果包含：1. 是否秒传成功；2. 如果没成功，已上传的分片索引列表
        return Result.success(fileService.checkFile(md5));
    }

    /**
     * 上传分片
     */
    @PostMapping("/upload-chunk")
    public Result uploadChunk(@RequestParam MultipartFile file,
                              @RequestParam String md5,
                              @RequestParam Integer index) {
        fileService.uploadChunk(file, md5, index);
        return Result.success("ok");
    }

    /**
     * 合并分片
     */
    @PostMapping("/merge")
    public Result merge(@RequestBody FileMergeRequest req) {
        // 现在需要传入 5 个参数
        fileService.mergeChunks(
                req.getMd5(),
                req.getFileName(),
                req.getTotalChunks(),
                req.getParentId()
        );
        return Result.success("合并成功");
    }


    // 2. 新增：更新描述接口
    @PutMapping("/description/{id}")
    public Result updateDescription(@PathVariable("id") Long id, @RequestParam String description) {
        fileService.updateFileDescription(id, description);
        return Result.success("描述更新成功");
    }

    /**
     * 获取文件列表（支持文件夹层级）
     */
    @GetMapping("/list")
    public Result listFiles(@RequestParam Long parentId) {
        Long userId = SecurityUtils.getUserId();
        return Result.success(fileService.getUserFiles(userId, parentId));
    }



    @PostMapping("/create-dir")
    public Result createDir(@RequestParam String name, @RequestParam Long parentId) {
        fileService.createDirectory(name, parentId);
        return Result.success("ok");
    }

    /**
     * 删除文件
     * @param id 文件记录的ID
     */
    @DeleteMapping("/{id}")
    public Result deleteFile(@PathVariable("id") Long id) {
        // SecurityUtils 会从 SecurityContext 中解析当前登录的 userId
        Long userId = SecurityUtils.getUserId();
        fileService.deleteFile(id, userId);
        return Result.success("删除成功");
    }
}