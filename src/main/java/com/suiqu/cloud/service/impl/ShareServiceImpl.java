package com.suiqu.cloud.service.impl;

import com.suiqu.cloud.entity.FileUser;
import com.suiqu.cloud.entity.Share;
import com.suiqu.cloud.entity.dto.ShareCreateRequest;
import com.suiqu.cloud.entity.vo.ShareVO;
import com.suiqu.cloud.mapper.FileUserMapper;
import com.suiqu.cloud.mapper.ShareMapper;
import com.suiqu.cloud.service.ShareService;
import com.suiqu.cloud.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ShareServiceImpl implements ShareService {

    @Autowired private ShareMapper shareMapper;
    @Autowired private RedissonClient redissonClient;
    @Autowired private FileUserMapper fileUserMapper; // 逻辑表
    @Autowired private RBloomFilter<Long> shareBloomFilter;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShareVO createShare(ShareCreateRequest req) {
        // 1. 查询逻辑文件信息
        FileUser userFile = fileUserMapper.selectById(req.getFileId());
        if (userFile == null) {
            throw new RuntimeException("文件记录不存在");
        }

        // 2. 存入分享表
        Share share = new Share();
        share.setFileId(req.getFileId()); // 存逻辑文件ID
        share.setUserId(SecurityUtils.getUserId());
        share.setExpireTime(LocalDateTime.now().plusHours(req.getExpireHours()));
        share.setStatus(0);
        share.setCreateTime(LocalDateTime.now());
        shareMapper.insert(share);

        // 3. 加入布隆过滤器 (防止缓存穿透)
        shareBloomFilter.add(share.getId());

        // 4. 放入延时队列 (处理过期)
        RBlockingQueue<Long> blockingQueue = redissonClient.getBlockingQueue("share:expired:queue");
        RDelayedQueue<Long> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        delayedQueue.offer(share.getId(), req.getExpireHours(), TimeUnit.HOURS);

        // 5. 组装返回 VO
        return ShareVO.builder()
                .id(share.getId())
                .shareUrl("/share/view/" + share.getId())
                .expireTime(share.getExpireTime())
                .status(0)
                .fileName(userFile.getFileName())
                .build();
    }

    @Override
    public ShareVO getShareDetails(String shareIdStr) {
        Long shareId = Long.parseLong(shareIdStr);

        // 1. 布隆过滤器拦截：如果过滤器说没有，那绝对没有
        if (!shareBloomFilter.contains(shareId)) {
            log.warn("布隆过滤器拦截非法请求分享ID: {}", shareId);
            return null;
        }

        // 2. 调用关联查询 (三表联查: share + file_user + file_info)
        ShareVO shareVO = shareMapper.selectShareDetail(shareId);

        // 3. 校验状态与过期时间
        if (shareVO == null || Integer.valueOf(1).equals(shareVO.getStatus())
                || shareVO.getExpireTime().isBefore(LocalDateTime.now())) {
            return null;
        }

        shareVO.setShareUrl("/share/view/" + shareId);
        return shareVO;
    }

    @Override
    public void expireShare(Long shareId) {
        log.info("执行过期任务，分享ID: {}", shareId);
        Share share = new Share();
        share.setId(shareId);
        share.setStatus(1);
        shareMapper.updateById(share);
    }
}