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

@Component
public class FileSyncConsumer {
    @Autowired
    private ElasticsearchOperations esOperations;

    @RabbitListener(queues = RabbitConfig.SYNC_QUEUE)
    public void handleCanalMessage(String message) {
        // Canal发送的消息通常是JSON格式，包含data、type(INSERT/UPDATE/DELETE)
        System.out.println("收到 Canal 同步消息: " + message); // 这一行非常关键
        JSONObject json = JSON.parseObject(message);
        String type = json.getString("type");
        JSONArray data = json.getJSONArray("data");

        for (int i = 0; i < data.size(); i++) {
            FileIndex index = data.getObject(i, FileIndex.class);
            if ("DELETE".equals(type)) {
                esOperations.delete(index.getId().toString(), FileIndex.class);
            } else {
                esOperations.save(index);
            }
        }
    }
}
