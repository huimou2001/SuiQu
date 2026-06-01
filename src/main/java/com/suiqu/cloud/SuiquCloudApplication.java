package com.suiqu.cloud;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.suiqu.cloud.mapper")
@EnableScheduling // 开启定时任务支持
public class SuiquCloudApplication {
    public static void main(String[] args) {
        SpringApplication.run(SuiquCloudApplication.class, args);
    }
}