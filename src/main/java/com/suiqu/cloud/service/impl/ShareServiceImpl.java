package com.suiqu.cloud.service.impl;

import com.suiqu.cloud.entity.FileInfo;
import com.suiqu.cloud.entity.Share;
import com.suiqu.cloud.entity.dto.ShareCreateRequest;
import com.suiqu.cloud.entity.vo.ShareVO;
import com.suiqu.cloud.mapper.FileMapper;
import com.suiqu.cloud.mapper.ShareMapper;
import com.suiqu.cloud.service.ShareService;
import com.suiqu.cloud.utils.SecurityUtils;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ShareServiceImpl implements ShareService {

    @Autowired
    private ShareMapper shareMapper;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private RBloomFilter<Long> shareBloomFilter;
    @Override
    @Transactional
    public ShareVO createShare(ShareCreateRequest req) {
        // 1. 查询文件元信息（为了给 VO 赋值）
        FileInfo file = fileMapper.selectById(req.getFileId());
        if (file == null) {
            throw new RuntimeException("文件不存在");
        }

        // 2. 存入分享表
        Share share = new Share();
        share.setFileId(req.getFileId());
        share.setUserId(SecurityUtils.getUserId());
        share.setExpireTime(LocalDateTime.now().plusHours(req.getExpireHours()));
        share.setStatus(0);
        share.setCreateTime(LocalDateTime.now());
        shareMapper.insert(share);

        //加入到布隆过滤器
        shareBloomFilter.add(share.getId());
        // 3. 放入延时队列
        RBlockingQueue<Long> blockingQueue = redissonClient.getBlockingQueue("share:expired:queue");
        RDelayedQueue<Long> delayedQueue = redissonClient.getDelayedQueue(blockingQueue);
        delayedQueue.offer(share.getId(), req.getExpireHours(), TimeUnit.HOURS);

        // 4. 返回完整的 VO（手动传入 7 个参数）
        return new ShareVO(
                share.getId(),
                "/share/view/" + share.getId(),
                share.getExpireTime(),
                share.getStatus(),
                file.getName(),
                file.getSize(),
                file.getType()
        );
    }

    @Override
    public void expireShare(Long shareId) {
        log.info("执行过期任务，分享ID: {}", shareId);
        Share share = new Share();
        share.setId(shareId);
        share.setStatus(1); // 1-已失效
        shareMapper.updateById(share);
    }

    @Override
    public ShareVO getShareDetails(String shareId) {
        // 1. 直接调用 Mapper 的关联查询方法
        ShareVO shareVO = shareMapper.selectShareWithFile(Long.parseLong(shareId));

        // 2. 校验分享是否存在、是否已失效、是否已过期
        if (shareVO == null || Integer.valueOf(1).equals(shareVO.getStatus())
                || shareVO.getExpireTime().isBefore(LocalDateTime.now())) {
            return null;
        }

        // 3. 补充数据库里没有的动态字段：分享链接
        shareVO.setShareUrl("/share/view/" + shareId);

        return shareVO;
    }
}
