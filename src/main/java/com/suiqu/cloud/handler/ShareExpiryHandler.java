package com.suiqu.cloud.handler;

import com.suiqu.cloud.service.ShareService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ShareExpiryHandler implements CommandLineRunner {

    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private ShareService shareService;

    @Override
    public void run(String... args) {
        new Thread(() -> {
            // 获取阻塞队列
            RBlockingQueue<Long> blockingQueue = redissonClient.getBlockingQueue("share:expired:queue");
            while (true) {
                try {
                    // 阻塞获取到期的分享ID
                    Long shareId = blockingQueue.take();
                    shareService.expireShare(shareId);
                } catch (InterruptedException e) {
                    log.error("延时队列监听中断");
                    break;
                }
            }
        }, "Share-Expiry-Thread").start();
    }
}
