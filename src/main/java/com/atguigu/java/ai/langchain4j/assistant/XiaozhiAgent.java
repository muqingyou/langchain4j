package com.atguigu.java.ai.langchain4j.assistant;

import dev.langchain4j.service.*;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

import static dev.langchain4j.service.spring.AiServiceWiringMode.EXPLICIT;

@AiService(
        wiringMode = EXPLICIT,
//        chatModel = "qwenChatModel",
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProviderXiaozhi",
        tools = {"appointmentTools", "baiduSearchTools"}, //tools配置
        contentRetriever = "contentRetrieverXiaozhiPincone" //配置向量存储
)
public interface XiaozhiAgent {
    @SystemMessage(fromResource = "zhaozhi-prompt-template.txt")
    Flux<String> chat(@MemoryId Long memoryId, @UserMessage String userMessage);
//    String chat(@MemoryId Long memoryId, @UserMessage String userMessage);
    
    // 使用层级递归总结的记忆压缩方法
    @SystemMessage(fromResource = "zhaozhi-prompt-template.txt")
    Flux<String> chatWithHierarchicalSummarization(@MemoryId Long memoryId, @UserMessage String userMessage);
}