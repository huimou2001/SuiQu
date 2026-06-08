package com.suiqu.cloud.service;

import com.suiqu.cloud.entity.FileIndex;
import com.suiqu.cloud.mapper.FileUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class ElasticsearchSyncService {

    @Autowired
    private FileUserMapper fileUserMapper;

    @Autowired
    private ElasticsearchOperations esOperations;

    public void syncAllData() {
        log.info(">>> 开始全量对齐 MySQL 与 Elasticsearch 数据...");

        try {
            // 1. 获取索引操作对象
            IndexOperations indexOps = esOperations.indexOps(FileIndex.class);

            // 2. 全量对齐前，先删除旧索引以清除冗余数据
            if (indexOps.exists()) {
                indexOps.delete();
                log.info(">>> 已清除 ES 旧索引数据");
            }
            indexOps.create();
            indexOps.putMapping();

            // 3. 从 MySQL 获取最新数据
            List<FileIndex> allFiles = fileUserMapper.selectAllForSync();

            if (allFiles == null || allFiles.isEmpty()) {
                log.info(">>> MySQL 中无文件数据，ES 已清空。");
                return;
            }

            // 4. 批量写入 ES
            esOperations.save(allFiles);

            log.info(">>> 全量同步成功，共同步 {} 条记录。", allFiles.size());
        } catch (Exception e) {
            log.error(">>> 数据对齐失败: ", e);
        }
    }
}