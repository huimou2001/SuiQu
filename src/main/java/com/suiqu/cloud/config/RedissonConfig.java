package com.suiqu.cloud.config;

import org.redisson.Redisson;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {
    @Value("${spring.data.redis.host}") private String host;
    @Value("${spring.data.redis.port}") private String port;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer().setAddress("redis://" + host + ":" + port);
        return Redisson.create(config);
    }

    @Bean
    public RBloomFilter<Long> shareBloomFilter(RedissonClient redissonClient) {
        // 定义布隆过滤器的名字
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter("share:bloom:filter");

        // 初始化：预计插入100万条数据，允许误差率 3%
        // 注意：初次启动项目时需要初始化，一旦初始化后不能随意更改参数
        bloomFilter.tryInit(1000000L, 0.03);
        return bloomFilter;
    }
}
