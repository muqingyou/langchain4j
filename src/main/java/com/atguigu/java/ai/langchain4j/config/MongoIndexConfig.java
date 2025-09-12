package com.atguigu.java.ai.langchain4j.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

/**
 * MongoDB索引配置
 * 用于优化查询性能
 */
@Component
public class MongoIndexConfig implements CommandLineRunner {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) throws Exception {
        createIndexes();
    }

    /**
     * 创建MongoDB索引
     */
    private void createIndexes() {
        try {
            // 为chat_messages集合创建索引
            IndexOperations indexOps = mongoTemplate.indexOps("chat_messages");
            
            // 为memoryId字段创建索引（用于快速查找特定用户的聊天记录）
            Index memoryIdIndex = new Index().on("memoryId", org.springframework.data.domain.Sort.Direction.ASC);
            indexOps.ensureIndex(memoryIdIndex);
            
            // 为messageId字段创建唯一索引（确保消息ID唯一性）
            Index messageIdIndex = new Index().on("messageId", org.springframework.data.domain.Sort.Direction.ASC).unique();
            indexOps.ensureIndex(messageIdIndex);
            
            System.out.println("MongoDB索引创建完成");
        } catch (Exception e) {
            System.err.println("创建MongoDB索引时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
