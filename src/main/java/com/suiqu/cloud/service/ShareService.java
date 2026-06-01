package com.suiqu.cloud.service;

import com.suiqu.cloud.entity.dto.ShareCreateRequest;
import com.suiqu.cloud.entity.vo.ShareVO;

public interface ShareService {
    ShareVO createShare(ShareCreateRequest req);
    void expireShare(Long shareId);
    ShareVO getShareDetails(String shareId);
}