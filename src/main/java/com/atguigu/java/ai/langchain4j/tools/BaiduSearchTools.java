package com.atguigu.java.ai.langchain4j.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BaiduSearchTools {
    
    @Value("${bing.search.api.key}")
    private String apiKey;
    
    @Value("${bing.search.api.url}")
    private String apiUrl;
    
    @Value("${bing.search.api.count}")
    private int resultCount;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public BaiduSearchTools() {
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();
        this.objectMapper = new ObjectMapper();
    }
    
    @Tool(name = "搜索医疗信息", value = "使用Bing搜索API搜索最新的医疗信息、疾病症状、治疗方法、医院科室等相关信息，为分导诊提供更准确的信息支持")
    public String searchMedicalInfo(@P(value = "搜索关键词，如疾病名称、症状、科室名称等") String query) {
        try {
            String searchQuery = query + " 医疗 医院 科室 症状 治疗";
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(apiUrl)
                            .queryParam("q", searchQuery)
                            .queryParam("count", resultCount)
                            .queryParam("mkt", "zh-CN")
                            .queryParam("safesearch", "Moderate")
                            .build())
                    .header("Ocp-Apim-Subscription-Key", apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return parseSearchResults(response, query);
            
        } catch (Exception e) {
            return "搜索医疗信息时发生错误：" + e.getMessage() + "。请尝试使用其他方式获取信息。";
        }
    }
    
    @Tool(name = "搜索科室信息", value = "搜索特定科室的详细信息，包括科室职责、常见疾病、医生信息等")
    public String searchDepartmentInfo(@P(value = "科室名称") String departmentName) {
        try {
            String searchQuery = departmentName + " 科室 职责 常见疾病 医生 北京协和医院";
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(apiUrl)
                            .queryParam("q", searchQuery)
                            .queryParam("count", resultCount)
                            .queryParam("mkt", "zh-CN")
                            .queryParam("safesearch", "Moderate")
                            .build())
                    .header("Ocp-Apim-Subscription-Key", apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return parseSearchResults(response, departmentName);
            
        } catch (Exception e) {
            return "搜索科室信息时发生错误：" + e.getMessage() + "。请尝试使用其他方式获取信息。";
        }
    }
    
    @Tool(name = "搜索疾病症状", value = "搜索特定疾病的症状、病因、诊断方法、治疗方案等信息")
    public String searchDiseaseSymptoms(@P(value = "疾病名称或症状描述") String diseaseOrSymptom) {
        try {
            String searchQuery = diseaseOrSymptom + " 症状 病因 诊断 治疗 医院";
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(apiUrl)
                            .queryParam("q", searchQuery)
                            .queryParam("count", resultCount)
                            .queryParam("mkt", "zh-CN")
                            .queryParam("safesearch", "Moderate")
                            .build())
                    .header("Ocp-Apim-Subscription-Key", apiKey)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return parseSearchResults(response, diseaseOrSymptom);
            
        } catch (Exception e) {
            return "搜索疾病症状时发生错误：" + e.getMessage() + "。请尝试使用其他方式获取信息。";
        }
    }
    
    @Tool(name = "智能分导诊", value = "根据患者症状，使用百度搜索API获取医疗信息并进行分导诊分析，推荐最合适的科室和医生")
    public String intelligentMedicalGuidance(@P(value = "患者症状描述") String symptoms) {
        try {
            // 1. 调用内部搜索方法获取医疗信息
            String medicalInfo = searchMedicalInfoInternal(symptoms);
            String diseaseInfo = searchDiseaseSymptomsInternal(symptoms);
            
            // 2. 推荐科室
            String recommendedDepartment = recommendDepartment(symptoms);
            String departmentInfo = searchDepartmentInfoInternal(recommendedDepartment);
            
            // 3. 构建分导诊结论
            return buildGuidanceResult(symptoms, medicalInfo, diseaseInfo, recommendedDepartment, departmentInfo);
            
        } catch (Exception e) {
            return "❌ 分导诊服务暂时不可用，请稍后重试或直接咨询医生。错误信息：" + e.getMessage();
        }
    }
    
    // 内部搜索方法（不暴露为工具）
    private String searchMedicalInfoInternal(String query) {
        try {
            String searchQuery = query + " 医疗 医院 科室 症状 治疗";
            String response = performSearch(searchQuery);
            return parseSearchResults(response, query);
        } catch (Exception e) {
            return "搜索医疗信息时发生错误：" + e.getMessage();
        }
    }
    
    private String searchDepartmentInfoInternal(String departmentName) {
        try {
            String searchQuery = departmentName + " 科室 职责 常见疾病 医生 北京协和医院";
            String response = performSearch(searchQuery);
            return parseSearchResults(response, departmentName);
        } catch (Exception e) {
            return "搜索科室信息时发生错误：" + e.getMessage();
        }
    }
    
    private String searchDiseaseSymptomsInternal(String diseaseOrSymptom) {
        try {
            String searchQuery = diseaseOrSymptom + " 症状 病因 诊断 治疗 医院";
            String response = performSearch(searchQuery);
            return parseSearchResults(response, diseaseOrSymptom);
        } catch (Exception e) {
            return "搜索疾病症状时发生错误：" + e.getMessage();
        }
    }
    
    // 通用搜索方法
    private String performSearch(String searchQuery) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(apiUrl)
                        .queryParam("q", searchQuery)
                        .queryParam("count", resultCount)
                        .queryParam("mkt", "zh-CN")
                        .queryParam("safesearch", "Moderate")
                        .build())
                .header("Ocp-Apim-Subscription-Key", apiKey)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
    
    // 智能科室推荐逻辑
    private String recommendDepartment(String symptoms) {
        try {
            // 1. 首先通过搜索API获取专业的医疗分析
            String medicalAnalysis = searchMedicalInfoInternal(symptoms + " 科室推荐 分导诊");
            
            // 2. 基于症状进行多维度分析
            DepartmentRecommendation recommendation = analyzeSymptoms(symptoms, medicalAnalysis);
            
            // 3. 返回推荐结果，包含置信度
            return formatRecommendation(recommendation);
            
        } catch (Exception e) {
            // 如果智能分析失败，回退到基础分析
            return fallbackDepartmentRecommendation(symptoms);
        }
    }
    
    // 症状分析类
    private static class DepartmentRecommendation {
        private final String primaryDepartment;
        private final String secondaryDepartment;
        private final int confidence;
        private final String reasoning;
        private final List<String> relatedSymptoms;
        
        public DepartmentRecommendation(String primary, String secondary, int confidence, String reasoning, List<String> symptoms) {
            this.primaryDepartment = primary;
            this.secondaryDepartment = secondary;
            this.confidence = confidence;
            this.reasoning = reasoning;
            this.relatedSymptoms = symptoms;
        }
        
        // Getters
        public String getPrimaryDepartment() { return primaryDepartment; }
        public String getSecondaryDepartment() { return secondaryDepartment; }
        public int getConfidence() { return confidence; }
        public String getReasoning() { return reasoning; }
        public List<String> getRelatedSymptoms() { return relatedSymptoms; }
    }
    
    // 多维度症状分析
    private DepartmentRecommendation analyzeSymptoms(String symptoms, String medicalAnalysis) {
        String lowerSymptoms = symptoms.toLowerCase();
        List<String> detectedSymptoms = new ArrayList<>();
        Map<String, Integer> departmentScores = new HashMap<>();
        
        // 定义症状-科室映射关系（更详细的映射）
        Map<String, List<String>> symptomDepartmentMap = createSymptomDepartmentMapping();
        
        // 分析症状并计算科室得分
        for (Map.Entry<String, List<String>> entry : symptomDepartmentMap.entrySet()) {
            String department = entry.getKey();
            List<String> departmentSymptoms = entry.getValue();
            
            int score = 0;
            for (String symptom : departmentSymptoms) {
                if (lowerSymptoms.contains(symptom)) {
                    score += getSymptomWeight(symptom);
                    detectedSymptoms.add(symptom);
                }
            }
            departmentScores.put(department, score);
        }
        
        // 特殊症状组合分析
        scoreSpecialCombinations(lowerSymptoms, departmentScores, detectedSymptoms);
        
        // 基于搜索结果的额外分析
        analyzeSearchResults(medicalAnalysis, departmentScores);
        
        // 选择得分最高的科室
        String primaryDepartment = departmentScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("内科");
        
        // 选择第二高的科室
        String secondaryDepartment = departmentScores.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(primaryDepartment))
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        
        // 计算置信度
        int maxScore = departmentScores.get(primaryDepartment);
        int confidence = Math.min(100, (maxScore * 100) / 50); // 假设最大可能得分为50
        
        // 生成推理说明
        String reasoning = generateReasoning(primaryDepartment, detectedSymptoms, maxScore);
        
        return new DepartmentRecommendation(primaryDepartment, secondaryDepartment, confidence, reasoning, detectedSymptoms);
    }
    
    // 创建症状-科室映射关系
    private Map<String, List<String>> createSymptomDepartmentMapping() {
        Map<String, List<String>> mapping = new HashMap<>();
        
        // 神经内科
        mapping.put("神经内科", Arrays.asList(
            "头痛", "头晕", "眩晕", "偏头痛", "神经痛", "癫痫", "抽搐", "意识障碍",
            "记忆力减退", "认知障碍", "帕金森", "脑梗", "中风", "面瘫", "肢体麻木"
        ));
        
        // 心内科
        mapping.put("心内科", Arrays.asList(
            "胸痛", "胸闷", "心悸", "心慌", "气短", "呼吸困难", "心绞痛", "心律不齐",
            "高血压", "低血压", "心衰", "心肌梗死", "冠心病", "房颤", "早搏"
        ));
        
        // 消化内科
        mapping.put("消化内科", Arrays.asList(
            "腹痛", "胃痛", "胃胀", "恶心", "呕吐", "腹泻", "便秘", "便血", "黑便",
            "黄疸", "肝区痛", "胆囊痛", "消化不良", "反酸", "烧心", "嗳气"
        ));
        
        // 呼吸内科
        mapping.put("呼吸内科", Arrays.asList(
            "咳嗽", "咳痰", "咯血", "胸痛", "气短", "呼吸困难", "哮喘", "肺炎",
            "支气管炎", "肺气肿", "肺结核", "肺癌", "胸腔积液", "气胸"
        ));
        
        // 皮肤科
        mapping.put("皮肤科", Arrays.asList(
            "皮疹", "瘙痒", "红斑", "水疱", "脱皮", "皮肤干燥", "皮肤过敏", "湿疹",
            "皮炎", "荨麻疹", "痤疮", "白癜风", "银屑病", "真菌感染"
        ));
        
        // 眼科
        mapping.put("眼科", Arrays.asList(
            "视力下降", "视力模糊", "眼痛", "眼红", "眼干", "流泪", "眼痒", "飞蚊症",
            "青光眼", "白内障", "结膜炎", "角膜炎", "视网膜病变", "斜视"
        ));
        
        // 耳鼻喉科
        mapping.put("耳鼻喉科", Arrays.asList(
            "耳痛", "耳鸣", "听力下降", "耳聋", "耳流脓", "鼻塞", "流鼻涕", "鼻血",
            "嗅觉减退", "咽痛", "声音嘶哑", "吞咽困难", "扁桃体炎", "中耳炎"
        ));
        
        // 口腔科
        mapping.put("口腔科", Arrays.asList(
            "牙痛", "牙龈出血", "口腔溃疡", "口臭", "牙齿松动", "颞下颌关节痛",
            "舌痛", "唇炎", "智齿", "龋齿", "牙周炎", "口腔癌"
        ));
        
        // 骨科
        mapping.put("骨科", Arrays.asList(
            "关节痛", "腰痛", "背痛", "颈痛", "骨折", "扭伤", "肌肉痛", "骨痛",
            "关节炎", "骨质疏松", "椎间盘突出", "颈椎病", "腰椎病", "肩周炎"
        ));
        
        // 泌尿外科
        mapping.put("泌尿外科", Arrays.asList(
            "尿频", "尿急", "尿痛", "血尿", "腰痛", "肾区痛", "前列腺", "阳痿",
            "早泄", "不育", "肾结石", "膀胱炎", "肾炎", "尿路感染"
        ));
        
        // 妇科
        mapping.put("妇科", Arrays.asList(
            "月经不调", "痛经", "白带异常", "阴道出血", "下腹痛", "不孕", "更年期",
            "子宫肌瘤", "卵巢囊肿", "宫颈炎", "盆腔炎", "阴道炎", "乳腺癌"
        ));
        
        return mapping;
    }
    
    // 获取症状权重
    private int getSymptomWeight(String symptom) {
        // 根据症状的重要性和特异性分配权重
        if (symptom.contains("痛") || symptom.contains("血") || symptom.contains("癌")) {
            return 3; // 高权重症状
        } else if (symptom.contains("炎") || symptom.contains("症") || symptom.contains("障碍")) {
            return 2; // 中权重症状
        } else {
            return 1; // 低权重症状
        }
    }
    
    // 特殊症状组合分析
    private void scoreSpecialCombinations(String symptoms, Map<String, Integer> departmentScores, List<String> detectedSymptoms) {
        // 胸痛 + 呼吸困难 -> 心内科或呼吸内科
        if (symptoms.contains("胸痛") && symptoms.contains("呼吸困难")) {
            departmentScores.put("心内科", departmentScores.getOrDefault("心内科", 0) + 5);
            departmentScores.put("呼吸内科", departmentScores.getOrDefault("呼吸内科", 0) + 3);
            detectedSymptoms.add("胸痛+呼吸困难组合");
        }
        
        // 头痛 + 恶心呕吐 -> 神经内科
        if (symptoms.contains("头痛") && (symptoms.contains("恶心") || symptoms.contains("呕吐"))) {
            departmentScores.put("神经内科", departmentScores.getOrDefault("神经内科", 0) + 4);
            detectedSymptoms.add("头痛+恶心呕吐组合");
        }
        
        // 腹痛 + 发热 -> 消化内科或外科
        if (symptoms.contains("腹痛") && symptoms.contains("发热")) {
            departmentScores.put("消化内科", departmentScores.getOrDefault("消化内科", 0) + 3);
            detectedSymptoms.add("腹痛+发热组合");
        }
        
        // 关节痛 + 晨僵 -> 风湿免疫科
        if (symptoms.contains("关节痛") && symptoms.contains("晨僵")) {
            departmentScores.put("风湿免疫科", departmentScores.getOrDefault("风湿免疫科", 0) + 4);
            detectedSymptoms.add("关节痛+晨僵组合");
        }
    }
    
    // 基于搜索结果的分析
    private void analyzeSearchResults(String medicalAnalysis, Map<String, Integer> departmentScores) {
        if (medicalAnalysis == null || medicalAnalysis.isEmpty()) return;
        
        String lowerAnalysis = medicalAnalysis.toLowerCase();
        
        // 根据搜索结果中的关键词调整科室得分
        for (String department : departmentScores.keySet()) {
            if (lowerAnalysis.contains(department.toLowerCase())) {
                departmentScores.put(department, departmentScores.get(department) + 2);
            }
        }
    }
    
    // 生成推理说明
    private String generateReasoning(String department, List<String> symptoms, int score) {
        StringBuilder reasoning = new StringBuilder();
        reasoning.append("基于症状分析，推荐").append(department).append("：\n");
        reasoning.append("- 检测到相关症状：").append(String.join("、", symptoms)).append("\n");
        reasoning.append("- 匹配得分：").append(score).append("分\n");
        
        if (score >= 10) {
            reasoning.append("- 推荐强度：强烈推荐");
        } else if (score >= 5) {
            reasoning.append("- 推荐强度：推荐");
        } else {
            reasoning.append("- 推荐强度：建议咨询");
        }
        
        return reasoning.toString();
    }
    
    // 格式化推荐结果
    private String formatRecommendation(DepartmentRecommendation recommendation) {
        StringBuilder result = new StringBuilder();
        result.append("🎯 **推荐科室**：").append(recommendation.getPrimaryDepartment()).append("\n");
        result.append("📊 **推荐置信度**：").append(recommendation.getConfidence()).append("%\n");
        
        if (recommendation.getSecondaryDepartment() != null) {
            result.append("🔄 **备选科室**：").append(recommendation.getSecondaryDepartment()).append("\n");
        }
        
        result.append("💡 **分析依据**：\n").append(recommendation.getReasoning());
        
        return result.toString();
    }
    
    // 回退推荐方法
    private String fallbackDepartmentRecommendation(String symptoms) {
        String lowerSymptoms = symptoms.toLowerCase();
        
        if (lowerSymptoms.contains("头痛") || lowerSymptoms.contains("头晕") || lowerSymptoms.contains("神经")) {
            return "神经内科（基础分析）";
        } else if (lowerSymptoms.contains("心脏") || lowerSymptoms.contains("胸痛") || lowerSymptoms.contains("心悸")) {
            return "心内科（基础分析）";
        } else if (lowerSymptoms.contains("胃") || lowerSymptoms.contains("腹痛") || lowerSymptoms.contains("消化")) {
            return "消化内科（基础分析）";
        } else if (lowerSymptoms.contains("呼吸") || lowerSymptoms.contains("咳嗽") || lowerSymptoms.contains("胸闷")) {
            return "呼吸内科（基础分析）";
        } else if (lowerSymptoms.contains("皮肤") || lowerSymptoms.contains("皮疹") || lowerSymptoms.contains("瘙痒")) {
            return "皮肤科（基础分析）";
        } else if (lowerSymptoms.contains("眼睛") || lowerSymptoms.contains("视力") || lowerSymptoms.contains("眼科")) {
            return "眼科（基础分析）";
        } else if (lowerSymptoms.contains("耳朵") || lowerSymptoms.contains("听力") || lowerSymptoms.contains("耳鼻喉")) {
            return "耳鼻喉科（基础分析）";
        } else if (lowerSymptoms.contains("口腔") || lowerSymptoms.contains("牙齿") || lowerSymptoms.contains("牙科")) {
            return "口腔科（基础分析）";
        } else {
            return "内科（建议先到内科进行初步检查）";
        }
    }
    
    // 构建分导诊结果
    private String buildGuidanceResult(String symptoms, String medicalInfo, String diseaseInfo, 
                                     String recommendedDepartment, String departmentInfo) {
        StringBuilder result = new StringBuilder();
        
        result.append("🏥 **智能分导诊分析报告**\n\n");
        result.append("📋 **患者症状**：").append(symptoms).append("\n\n");
        
        // 推荐科室信息（现在包含更详细的分析）
        result.append(recommendedDepartment).append("\n\n");
        
        if (!medicalInfo.isEmpty() && !medicalInfo.contains("未找到")) {
            result.append("📚 **最新医疗信息**：\n").append(medicalInfo).append("\n\n");
        }
        
        if (!diseaseInfo.isEmpty() && !diseaseInfo.contains("未找到")) {
            result.append("🔍 **疾病症状分析**：\n").append(diseaseInfo).append("\n\n");
        }
        
        if (!departmentInfo.isEmpty() && !departmentInfo.contains("未找到")) {
            result.append("🏥 **科室详细信息**：\n").append(departmentInfo).append("\n\n");
        }
        
        result.append("💡 **就医建议**：\n");
        result.append("1. 请携带身份证和医保卡\n");
        result.append("2. 建议提前预约挂号\n");
        result.append("3. 如有紧急情况，请立即就医\n");
        result.append("4. 详细描述症状，配合医生诊断\n");
        result.append("5. 如有疑问，可咨询多个科室进行对比\n\n");
        
        result.append("⚠️ **重要提醒**：此分析仅供参考，不能替代专业医疗诊断。如有严重症状，请立即就医。");
        
        return result.toString();
    }
    
    private String parseSearchResults(String response, String originalQuery) {
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode webPages = rootNode.path("webPages").path("value");
            
            if (!webPages.isArray() || webPages.size() == 0) {
                return "未找到与 \"" + originalQuery + "\" 相关的搜索结果。";
            }
            
            StringBuilder result = new StringBuilder();
            result.append("🔍 搜索结果（基于 \"").append(originalQuery).append("\"）：\n\n");
            
            for (int i = 0; i < Math.min(webPages.size(), 3); i++) {
                JsonNode page = webPages.get(i);
                String title = page.path("name").asText();
                String snippet = page.path("snippet").asText();
                String url = page.path("url").asText();
                
                result.append("📋 ").append(title).append("\n");
                result.append("📝 ").append(snippet).append("\n");
                result.append("🔗 ").append(url).append("\n\n");
            }
            
            return result.toString();
            
        } catch (Exception e) {
            return "解析搜索结果时发生错误：" + e.getMessage();
        }
    }
}
