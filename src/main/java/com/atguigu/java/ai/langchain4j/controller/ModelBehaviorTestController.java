package com.atguigu.java.ai.langchain4j.controller;

import com.atguigu.java.ai.langchain4j.assistant.XiaozhiAgent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@Tag(name = "模型行为测试")
@RestController
@RequestMapping("/model-test")
public class ModelBehaviorTestController {
    
    @Autowired
    private XiaozhiAgent xiaozhiAgent;
    
    @Operation(summary = "测试模型独立判断能力", description = "测试模型不依赖工具时的判断能力")
    @PostMapping(value = "/independent-judgment", produces = "text/stream;charset=utf-8")
    public Flux<String> testIndependentJudgment(@RequestParam String question, @RequestParam(defaultValue = "1") Long memoryId) {
        String message = "请基于你的医学知识回答：" + question + "（不要使用任何工具，只基于你的知识回答）";
        return xiaozhiAgent.chat(memoryId, message);
    }
    
    @Operation(summary = "测试模型工具调用能力", description = "测试模型使用工具时的判断能力")
    @PostMapping(value = "/tool-assisted-judgment", produces = "text/stream;charset=utf-8")
    public Flux<String> testToolAssistedJudgment(@RequestParam String symptoms, @RequestParam(defaultValue = "1") Long memoryId) {
        String message = "我最近有这些症状：" + symptoms + "，请帮我分析一下应该挂哪个科室？";
        return xiaozhiAgent.chat(memoryId, message);
    }
    
    @Operation(summary = "测试模型综合判断能力", description = "测试模型结合工具和自身知识的综合判断")
    @PostMapping(value = "/comprehensive-judgment", produces = "text/stream;charset=utf-8")
    public Flux<String> testComprehensiveJudgment(@RequestParam String question, @RequestParam(defaultValue = "1") Long memoryId) {
        String message = "请结合你的医学知识和最新信息回答：" + question;
        return xiaozhiAgent.chat(memoryId, message);
    }
}

