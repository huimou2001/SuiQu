package com.suiqu.cloud.config;

import com.suiqu.cloud.service.ElasticsearchSyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DataSyncRunner implements CommandLineRunner {

    @Autowired
    private ElasticsearchSyncService syncService;

    @Override
    public void run(String... args) throws Exception {
        // 项目启动后执行全量同步
        syncService.syncAllData();
    }
}