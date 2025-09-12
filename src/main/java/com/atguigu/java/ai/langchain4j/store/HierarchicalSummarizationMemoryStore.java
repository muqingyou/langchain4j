package com.atguigu.java.ai.langchain4j.store;

import com.atguigu.java.ai.langchain4j.bean.ChatMessages;
import com.atguigu.java.ai.langchain4j.config.CompressionConfig;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import static java.util.stream.Collectors.toList;

@Component
public class HierarchicalSummarizationMemoryStore implements ChatMemoryStore {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    @Qualifier("qwenChatModel")
    private ChatLanguageModel chatLanguageModel;
    
    @Autowired
    private CompressionConfig compressionConfig;

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        ChatMessages chatMessages = mongoTemplate.findOne(query, ChatMessages.class);
        if (chatMessages == null) return new LinkedList<>();
        return ChatMessageDeserializer.messagesFromJson(chatMessages.getContent());
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        // 检查是否需要进行记忆压缩
        if (compressionConfig.isEnabled() && messages.size() > compressionConfig.getThreshold()) {
            messages = compressMessagesWithHierarchicalSummarization(messages);
        }

        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        Update update = new Update();
        update.set("content", ChatMessageSerializer.messagesToJson(messages));
        // 根据query条件能查询出文档，则修改文档；否则新增文档
        mongoTemplate.upsert(query, update, ChatMessages.class);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        Criteria criteria = Criteria.where("memoryId").is(memoryId);
        Query query = new Query(criteria);
        mongoTemplate.remove(query, ChatMessages.class);
    }

    /**
     * 使用层级递归总结进行消息压缩
     * 
     * @param messages 原始消息列表
     * @return 压缩后的消息列表
     */
    private List<ChatMessage> compressMessagesWithHierarchicalSummarization(List<ChatMessage> messages) {
        // 如果消息数量未超过阈值，则不进行压缩
        if (messages.size() <= compressionConfig.getThreshold()) {
            return messages;
        }

        // 分离需要保留的重要消息和需要压缩的消息
        List<ChatMessage> importantMessages = new ArrayList<>();
        List<ChatMessage> messagesToCompress = new ArrayList<>();
        
        // 保留系统消息和最近的几条重要消息
        for (int i = 0; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            
            // 始终保留系统消息
            if (message.type() == ChatMessageType.SYSTEM) {
                importantMessages.add(message);
            }
            // 保留最后几条消息（通常是最近的对话）
            else if (i >= messages.size() - compressionConfig.getKeepRecentMessages()) {
                importantMessages.add(message);
            }
            // 其他消息进行压缩
            else {
                messagesToCompress.add(message);
            }
        }

        // 如果不需要压缩的消息为空，直接返回原始消息
        if (messagesToCompress.isEmpty()) {
            return messages;
        }

        // 对需要压缩的消息进行层级递归总结
        List<ChatMessage> summarizedMessages = hierarchicalSummarize(messagesToCompress);
        
        // 合并重要消息和总结消息
        List<ChatMessage> result = new ArrayList<>();
        result.addAll(importantMessages);
        result.addAll(summarizedMessages);
        
        return result;
    }
    
    /**
     * 公共方法，供定时任务调用进行批量压缩
     * 
     * @param messages 原始消息列表
     * @return 压缩后的消息列表
     */
    public List<ChatMessage> compressMessagesForTest(List<ChatMessage> messages) {
        return compressMessagesWithHierarchicalSummarization(messages);
    }

    /**
     * 层级递归总结
     * 
     * @param messages 消息列表
     * @return 总结后的消息列表
     */
    private List<ChatMessage> hierarchicalSummarize(List<ChatMessage> messages) {
        // 如果消息数量较少，直接进行总结
        if (messages.size() <= compressionConfig.getChunkSize() * 2) {
            String summary = summarizeMessages(messages);
            return Collections.singletonList(AiMessage.from("【历史对话摘要】\n" + summary));
        }

        // 否则，分块处理
        List<String> summaries = new ArrayList<>();
        
        // 跳过系统消息（如果第一条是系统消息）
        int startIndex = (messages.get(0).type() == ChatMessageType.SYSTEM) ? 1 : 0;
        
        // 分块总结
        for (int i = startIndex; i < messages.size(); i += compressionConfig.getChunkSize()) {
            int endIndex = Math.min(i + compressionConfig.getChunkSize(), messages.size());
            List<ChatMessage> chunk = messages.subList(i, endIndex);
            String summary = summarizeMessages(chunk);
            summaries.add(summary);
        }

        // 递归处理总结结果
        List<ChatMessage> summaryMessages = new ArrayList<>();
        for (String summary : summaries) {
            summaryMessages.add(AiMessage.from(summary));
        }
        
        return hierarchicalSummarize(summaryMessages);
    }

    /**
     * 对一组消息进行总结
     * 
     * @param messages 消息列表
     * @return 总结内容
     */
    private String summarizeMessages(List<ChatMessage> messages) {
        if (messages.isEmpty()) {
            return "";
        }

        // 构建对话历史字符串
        StringBuilder historyBuilder = new StringBuilder();
        for (ChatMessage message : messages) {
            switch (message.type()) {
                case USER:
                    UserMessage userMessage = (UserMessage) message;
                    historyBuilder.append("用户: ").append(userMessage.singleText()).append("\n");
                    break;
                case AI:
                    AiMessage aiMessage = (AiMessage) message;
                    historyBuilder.append("助手: ").append(aiMessage.text()).append("\n");
                    break;
                case SYSTEM:
                    SystemMessage systemMessage = (SystemMessage) message;
                    historyBuilder.append("系统: ").append(systemMessage.text()).append("\n");
                    break;
                case TOOL_EXECUTION_RESULT:
                    ToolExecutionResultMessage toolMessage = (ToolExecutionResultMessage) message;
                    historyBuilder.append("工具执行结果: ").append(toolMessage.text()).append("\n");
                    break;
            }
        }

        // 构建医疗AI助手的专业总结提示词
        String prompt = buildMedicalSummarizationPrompt(historyBuilder.toString());

        // 使用LLM进行总结
        try {
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            // 如果LLM调用失败，返回原始文本
            e.printStackTrace();
            return "对话总结(降级处理): " + historyBuilder.toString();
        }
    }

    /**
     * 构建医疗AI助手的专业总结提示词
     * 
     * @param conversationHistory 对话历史
     * @return 完整的提示词
     */
    private String buildMedicalSummarizationPrompt(String conversationHistory) {
        return String.format("""
            你是一位专业的医疗AI助手，需要对以下医患对话进行智能总结。请按照以下要求进行总结：

            【总结要求】
            1. 保留所有重要的医疗信息，包括：
               - 患者的主要症状和病情描述
               - 疾病诊断信息
               - 用药信息（药物名称、剂量、用法）
               - 检查结果和检验数据
               - 治疗建议和医嘱
               - 复诊时间和注意事项

            2. 保留关键的预约信息：
               - 预约科室和医生
               - 预约时间
               - 预约状态

            3. 保留系统消息和重要的系统提示

            4. 总结格式要求：
               - 使用简洁明了的语言
               - 保持医疗信息的准确性和完整性
               - 按时间顺序整理关键信息
               - 突出重要的医疗决策和建议

            【对话内容】
            %s

            【总结】
            请基于以上对话内容，生成一个专业的医疗对话总结：
            """, conversationHistory);
    }
}