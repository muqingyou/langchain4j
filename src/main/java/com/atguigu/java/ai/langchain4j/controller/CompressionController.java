package com.atguigu.java.ai.langchain4j.controller;

import com.atguigu.java.ai.langchain4j.task.HierarchicalSummarizationTask;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 压缩任务控制器
 * 提供手动触发压缩任务的接口
 */
@RestController
@RequestMapping("/api/compression")
@Tag(name = "压缩任务管理", description = "聊天记录压缩相关接口")
public class CompressionController {

    @Autowired
    private HierarchicalSummarizationTask compressionTask;

    @PostMapping("/trigger")
    @Operation(summary = "手动触发压缩任务", description = "立即执行一次聊天记录压缩任务")
    public ResponseEntity<Map<String, Object>> triggerCompression() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 记录开始时间
            long startTime = System.currentTimeMillis();
            
            // 执行压缩任务
            compressionTask.compressAllChatMemories();
            
            // 计算执行时间
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            result.put("success", true);
            result.put("message", "压缩任务执行完成");
            result.put("duration", duration + "ms");
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "压缩任务执行失败: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
