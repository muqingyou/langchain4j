package com.atguigu.java.ai.langchain4j.task;

import com.atguigu.java.ai.langchain4j.bean.ChatMessages;
import com.atguigu.java.ai.langchain4j.config.CompressionConfig;
import com.atguigu.java.ai.langchain4j.store.HierarchicalSummarizationMemoryStore;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class HierarchicalSummarizationTask {

    private static final Logger logger = LoggerFactory.getLogger(HierarchicalSummarizationTask.class);

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private HierarchicalSummarizationMemoryStore hierarchicalSummarizationMemoryStore;
    
    @Autowired
    private CompressionConfig compressionConfig;

    /**
     * 定时任务：根据配置执行层级递归总结的记忆压缩
     * 对所有用户的聊天记录进行检查和压缩
     */
    @Scheduled(cron = "#{@compressionConfig.schedule}")
    public void compressAllChatMemories() {
        // 检查是否启用压缩功能
        if (!compressionConfig.isEnabled()) {
            logger.debug("聊天记录压缩功能已禁用，跳过执行");
            return;
        }
        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        logger.info("开始执行层级递归总结的记忆压缩任务，开始时间: {}", startTime);

        long startTimeMillis = System.currentTimeMillis();
        int totalRecords = 0;
        int compressedCount = 0;
        int errorCount = 0;

        try {
            // 查询所有聊天记录
            List<ChatMessages> allChatMessages = mongoTemplate.findAll(ChatMessages.class);
            totalRecords = allChatMessages.size();
            
            logger.info("查询到 {} 条聊天记录，开始进行压缩处理", totalRecords);
            
            for (ChatMessages chatMessage : allChatMessages) {
                try {
                    // 获取当前聊天记录的消息列表
                    List<ChatMessage> messages = ChatMessageDeserializer.messagesFromJson(chatMessage.getContent());
                    
                    // 检查是否需要压缩（消息数量超过阈值）
                    if (messages.size() > compressionConfig.getThreshold()) {
                        if (compressionConfig.isVerboseLogging()) {
                            logger.debug("用户 memoryId={} 的消息数量 {} 超过阈值 {}，开始压缩", 
                                    chatMessage.getMemoryId(), messages.size(), compressionConfig.getThreshold());
                        }
                        
                        // 执行层级递归总结压缩
                        List<ChatMessage> compressedMessages = hierarchicalSummarizationMemoryStore.compressMessagesForTest(messages);
                        
                        // 更新聊天记录
                        chatMessage.setContent(ChatMessageSerializer.messagesToJson(compressedMessages));
                        mongoTemplate.save(chatMessage);
                        compressedCount++;
                        
                        logger.info("用户 memoryId={} 的聊天记录已压缩，压缩前消息数量：{}，压缩后消息数量：{}", 
                                chatMessage.getMemoryId(), messages.size(), compressedMessages.size());
                    } else {
                        if (compressionConfig.isVerboseLogging()) {
                            logger.debug("用户 memoryId={} 的消息数量 {} 未超过阈值 {}，跳过压缩", 
                                    chatMessage.getMemoryId(), messages.size(), compressionConfig.getThreshold());
                        }
                    }
                } catch (Exception e) {
                    errorCount++;
                    logger.error("处理用户 memoryId={} 的聊天记录时出错: {}", 
                            chatMessage.getMemoryId(), e.getMessage(), e);
                }
            }
            
            long endTimeMillis = System.currentTimeMillis();
            long duration = endTimeMillis - startTimeMillis;
            String endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            
            logger.info("层级递归总结的记忆压缩任务执行完成，结束时间: {}，总耗时: {}ms", endTime, duration);
            logger.info("处理统计 - 总记录数: {}，压缩记录数: {}，错误记录数: {}", 
                    totalRecords, compressedCount, errorCount);
                    
        } catch (Exception e) {
            logger.error("执行层级递归总结的记忆压缩任务时出错: {}", e.getMessage(), e);
        }
    }

}