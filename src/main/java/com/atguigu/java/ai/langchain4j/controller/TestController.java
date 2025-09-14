package com.atguigu.java.ai.langchain4j.controller;

import com.atguigu.java.ai.langchain4j.assistant.XiaozhiAgent;
import com.atguigu.java.ai.langchain4j.bean.ChatForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Tag(name = "功能测试")
@RestController
@RequestMapping("/test")
public class TestController {
    
    @Autowired
    private XiaozhiAgent xiaozhiAgent;
    
    @Operation(summary = "测试智能分导诊功能", description = "测试集成在BaiduSearchTools中的智能分导诊功能")
    @PostMapping(value = "/guidance", produces = "text/stream;charset=utf-8")
    public Flux<String> testGuidance(@RequestParam String symptoms, @RequestParam(defaultValue = "1") Long memoryId) {
        String message = "我最近有这些症状：" + symptoms + "，请帮我分析一下应该挂哪个科室？";
        return xiaozhiAgent.chat(memoryId, message);
    }
    
    @Operation(summary = "测试挂号功能", description = "测试AI挂号功能")
    @PostMapping(value = "/appointment", produces = "text/stream;charset=utf-8")
    public Flux<String> testAppointment(@RequestParam String message, @RequestParam(defaultValue = "1") Long memoryId) {
        return xiaozhiAgent.chat(memoryId, message);
    }
    
    @Operation(summary = "测试知识库检索", description = "测试RAG知识库检索功能")
    @PostMapping(value = "/knowledge", produces = "text/stream;charset=utf-8")
    public Flux<String> testKnowledge(@RequestParam String query, @RequestParam(defaultValue = "1") Long memoryId) {
        String message = "请帮我查询关于" + query + "的医疗信息";
        return xiaozhiAgent.chat(memoryId, message);
    }
    
    @Operation(summary = "测试记忆功能", description = "测试MongoDB记忆和压缩功能")
    @PostMapping(value = "/memory", produces = "text/stream;charset=utf-8")
    public Flux<String> testMemory(@RequestParam String message, @RequestParam(defaultValue = "1") Long memoryId) {
        return xiaozhiAgent.chatWithHierarchicalSummarization(memoryId, message);
    }
}
