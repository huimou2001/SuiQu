package com.suiqu.cloud.Initializer;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.suiqu.cloud.mapper.ShareMapper;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import com.suiqu.cloud.entity.Share;

import java.util.List;

@Component
@Slf4j
public class BloomFilterInitializer implements CommandLineRunner {

    @Autowired
    private ShareMapper shareMapper;
    @Autowired
    private RBloomFilter<Long> shareBloomFilter;

    @Override
    public void run(String... args) {
        log.info("开始预热分享ID布隆过滤器...");
        // 查询所有正常的分享ID
        List<Share> shares = shareMapper.selectList(new LambdaQueryWrapper<Share>()
                .select(Share::getId));

        shares.forEach(share -> shareBloomFilter.add(share.getId()));
        log.info("布隆过滤器预热完成，共加载 {} 条数据", shares.size());
    }
}
