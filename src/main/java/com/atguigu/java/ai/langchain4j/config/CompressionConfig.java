package com.atguigu.java.ai.langchain4j.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 聊天记录压缩配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "xiaozhi.compression")
public class CompressionConfig {
    
    /**
     * 压缩阈值，当消息数量超过此值时进行压缩
     */
    private int threshold = 10;
    
    /**
     * 每块消息的数量
     */
    private int chunkSize = 5;
    
    /**
     * 保留最近消息的数量
     */
    private int keepRecentMessages = 3;
    
    /**
     * 是否启用压缩功能
     */
    private boolean enabled = true;
    
    /**
     * 压缩任务执行间隔（cron表达式）
     */
    private String schedule = "0 0 * * * ?";
    
    /**
     * 是否启用详细日志
     */
    private boolean verboseLogging = false;
}
