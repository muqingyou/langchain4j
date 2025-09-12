package com.atguigu.java.ai.langchain4j;

import com.atguigu.java.ai.langchain4j.bean.ChatMessages;
import com.atguigu.java.ai.langchain4j.config.CompressionConfig;
import com.atguigu.java.ai.langchain4j.store.HierarchicalSummarizationMemoryStore;
import com.atguigu.java.ai.langchain4j.task.HierarchicalSummarizationTask;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 压缩任务测试类
 */
@SpringBootTest
@ActiveProfiles("test")
public class CompressionTaskTest {

    @Autowired
    private HierarchicalSummarizationTask compressionTask;

    @Autowired
    private HierarchicalSummarizationMemoryStore memoryStore;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private CompressionConfig compressionConfig;

    @BeforeEach
    void setUp() {
        // 清理测试数据
        mongoTemplate.dropCollection(ChatMessages.class);
    }

    @Test
    void testCompressionThreshold() {
        // 测试压缩阈值配置
        assertTrue(compressionConfig.getThreshold() > 0);
        assertTrue(compressionConfig.getChunkSize() > 0);
        assertTrue(compressionConfig.getKeepRecentMessages() > 0);
    }

    @Test
    void testMessageCompression() {
        // 创建测试聊天记录
        List<ChatMessage> messages = createTestMessages(15); // 超过阈值
        
        // 测试压缩功能
        List<ChatMessage> compressedMessages = memoryStore.compressMessagesForTest(messages);
        
        // 验证压缩结果
        assertNotNull(compressedMessages);
        assertTrue(compressedMessages.size() < messages.size());
        
        // 验证系统消息被保留
        assertTrue(compressedMessages.get(0).type().equals(dev.langchain4j.data.message.ChatMessageType.SYSTEM));
    }

    @Test
    void testNoCompressionWhenBelowThreshold() {
        // 创建少于阈值的消息
        List<ChatMessage> messages = createTestMessages(5);
        
        // 测试不压缩的情况
        List<ChatMessage> result = memoryStore.compressMessagesForTest(messages);
        
        // 验证没有压缩
        assertEquals(messages.size(), result.size());
    }

    @Test
    void testMongoDBCrud() {
        // 创建测试数据
        List<ChatMessage> messages = createTestMessages(12);
        String content = ChatMessageSerializer.messagesToJson(messages);
        
        ChatMessages chatMessages = new ChatMessages();
        chatMessages.setMemoryId(1);
        chatMessages.setContent(content);
        
        // 保存到MongoDB
        mongoTemplate.save(chatMessages);
        
        // 验证保存成功
        ChatMessages saved = mongoTemplate.findById(chatMessages.getMessageId(), ChatMessages.class);
        assertNotNull(saved);
        assertEquals(1, saved.getMemoryId());
        
        // 验证消息内容
        List<ChatMessage> retrievedMessages = ChatMessageDeserializer.messagesFromJson(saved.getContent());
        assertEquals(messages.size(), retrievedMessages.size());
    }

    @Test
    void testCompressionWithMedicalContent() {
        // 创建包含医疗信息的测试消息
        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("你是一位专业的医疗AI助手"));
        
        messages.add(UserMessage.from("我最近头痛，还伴有发热症状"));
        messages.add(AiMessage.from("根据您的症状描述，建议您尽快就医检查。头痛伴发热可能的原因包括感冒、流感等。"));
        
        messages.add(UserMessage.from("我昨天量体温是38.5度"));
        messages.add(AiMessage.from("38.5度属于低热，建议您多休息，多喝水，如果症状持续或加重请及时就医。"));
        
        messages.add(UserMessage.from("我想预约神经内科的医生"));
        messages.add(AiMessage.from("好的，我来帮您预约神经内科医生。请告诉我您希望预约的时间。"));
        
        // 测试压缩
        List<ChatMessage> compressed = memoryStore.compressMessagesForTest(messages);
        
        // 验证压缩结果包含医疗信息
        assertNotNull(compressed);
        assertTrue(compressed.size() < messages.size());
        
        // 验证系统消息被保留
        assertEquals(dev.langchain4j.data.message.ChatMessageType.SYSTEM, compressed.get(0).type());
    }

    /**
     * 创建测试消息
     */
    private List<ChatMessage> createTestMessages(int count) {
        List<ChatMessage> messages = new ArrayList<>();
        
        // 添加系统消息
        messages.add(SystemMessage.from("你是一位专业的医疗AI助手"));
        
        // 添加用户和AI消息
        for (int i = 1; i < count; i++) {
            if (i % 2 == 1) {
                messages.add(UserMessage.from("用户消息 " + i));
            } else {
                messages.add(AiMessage.from("AI回复 " + i));
            }
        }
        
        return messages;
    }
}
