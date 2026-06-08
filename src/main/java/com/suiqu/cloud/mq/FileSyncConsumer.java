package com.suiqu.cloud.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.suiqu.cloud.config.RabbitConfig;
import com.suiqu.cloud.entity.FileIndex;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Component
@Slf4j
public class FileSyncConsumer {
    @Autowired
    private ElasticsearchOperations esOperations;

    @RabbitListener(queues = RabbitConfig.SYNC_QUEUE)
    public void handleCanalMessage(String message) {
        log.info("收到 Canal 同步消息: {}", message);
        JSONObject json = JSON.parseObject(message);

//        // 1. 判断是否DDL语句(建库、建表、删表等)，DDL消息data一定为null，直接ACK丢弃不处理
//        Boolean isDdl = json.getBoolean("isDdl");
//        if (Boolean.TRUE.equals(isDdl)) {
//            log.info("当前为DDL语句，跳过ES同步，sql:{}", json.getString("sql"));
//            return;
//        }
//
//        // 2. 非DDL，获取data并做空判断，避免null调用size()空指针
//        JSONArray data = json.getJSONArray("data");
//        if (data == null || data.isEmpty()) {
//            log.warn("Canal消息data为空，跳过处理");
//            return;
//        }
//
//        // 3. 正常DML(INSERT/UPDATE/DELETE)执行ES同步
//        String type = json.getString("type");
//        for (int i = 0; i < data.size(); i++) {
//            FileIndex index = data.getObject(i, FileIndex.class);
//            if ("DELETE".equals(type)) {
//                esOperations.delete(index.getId().toString(), FileIndex.class);
//            } else {
//                esOperations.save(index);
//            }
//        }

        String table = json.getString("table");

        // 仅处理 file_user 表的增量，因为 FileIndex 结构对应的是逻辑文件
        if (!"file_user".equals(table)) {
            return;
        }

        Boolean isDdl = json.getBoolean("isDdl");
        if (Boolean.TRUE.equals(isDdl)) return;

        JSONArray data = json.getJSONArray("data");
        if (data == null || data.isEmpty()) return;

        String type = json.getString("type");
        for (int i = 0; i < data.size(); i++) {
            // FastJSON 的 getObject 有时无法处理 PropertyAlias，手动转换更稳
            JSONObject rowData = data.getJSONObject(i);

            if ("DELETE".equals(type)) {
                esOperations.delete(rowData.getString("id"), FileIndex.class);
            } else {
                FileIndex index = new FileIndex();
                index.setId(rowData.getLong("id"));
                index.setName(rowData.getString("file_name")); // 手动映射字段名
                index.setDescription(rowData.getString("description"));
                index.setUserId(rowData.getLong("user_id"));
                // 转换时间
                String createTimeStr = rowData.getString("create_time");
                if (createTimeStr != null) {
                    index.setCreateTime(LocalDateTime.parse(createTimeStr.replace(" ", "T")));
                }
                esOperations.save(index);
            }
        }



    }
}