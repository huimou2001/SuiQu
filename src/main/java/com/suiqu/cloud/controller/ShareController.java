package com.suiqu.cloud.controller;

import com.suiqu.cloud.entity.Result;
import com.suiqu.cloud.entity.dto.ShareCreateRequest;
import com.suiqu.cloud.entity.vo.ShareVO;
import com.suiqu.cloud.service.ShareService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/share")
public class ShareController {
    @Autowired
    private ShareService shareService;

    /**
     * 创建分享链接
     */
    @PostMapping("/create")
    public Result createShare(@RequestBody ShareCreateRequest req) {
        // req 包含 fileId, expireTime (小时)
        return Result.success(shareService.createShare(req));
    }

    /**
     * 查看分享详情（公共接口）
     */
    @GetMapping("/{shareId}")
    public Result getShareInfo(@PathVariable String shareId) {
        ShareVO share = shareService.getShareDetails(shareId);
        if (share == null || share.getStatus() == 1) {
            return Result.error("分享链接已失效");
        }
        return Result.success(share);
    }
}
