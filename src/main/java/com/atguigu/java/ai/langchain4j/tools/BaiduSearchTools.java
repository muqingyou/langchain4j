package com.atguigu.java.ai.langchain4j.tools;

import com.atguigu.java.ai.langchain4j.entity.SearchEvidence;
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
import java.util.List;

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
    public List<SearchEvidence> searchMedicalInfo(@P(value = "搜索关键词，如疾病名称、症状、科室名称等") String query) {
        try {
            String searchQuery = query + " 医疗 医院 科室 症状 治疗";
            String response = performSearch(searchQuery);
            return parseSearchResultsToEvidence(response, query, "medical_website");
        } catch (Exception e) {
            return List.of();
        }
    }
    
    @Tool(name = "搜索科室信息", value = "搜索特定科室的详细信息，包括科室职责、常见疾病、医生信息等")
    public List<SearchEvidence> searchDepartmentInfo(@P(value = "科室名称") String departmentName) {
        try {
            String searchQuery = departmentName + " 科室 职责 常见疾病 医生";
            String response = performSearch(searchQuery);
            return parseSearchResultsToEvidence(response, departmentName, "department_info");
        } catch (Exception e) {
            return List.of();
        }
    }
    
    @Tool(name = "搜索疾病症状", value = "搜索特定疾病的症状、病因、诊断方法、治疗方案等信息")
    public List<SearchEvidence> searchDiseaseSymptoms(@P(value = "疾病名称或症状描述") String diseaseOrSymptom) {
        try {
            String searchQuery = diseaseOrSymptom + " 症状 病因 诊断 治疗";
            String response = performSearch(searchQuery);
            return parseSearchResultsToEvidence(response, diseaseOrSymptom, "disease_info");
        } catch (Exception e) {
            return List.of();
        }
    }
    
    @Tool(name = "智能分导诊", value = "根据患者症状，调用搜索API获取医疗相关的网页摘要")
    public String intelligentMedicalGuidance(@P("患者症状描述") String symptoms) {
        try {
            // 直接搜索症状相关的医疗信息
            String searchQuery = symptoms + " 医疗 医院 科室 症状 治疗 诊断";
            String response = performSearch(searchQuery);
            List<SearchEvidence> evidences = parseSearchResultsToEvidence(response, symptoms, "medical_website");
            
            // 将 SearchEvidence 列表转换为格式化的字符串
            return formatSearchEvidenceAsString(evidences, symptoms);
        } catch (Exception e) {
            return "搜索医疗信息时发生错误：" + e.getMessage();
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
    
    /**
     * 将搜索结果解析为SearchEvidence列表
     */
    private List<SearchEvidence> parseSearchResultsToEvidence(String response, String originalQuery, String sourceType) {
        List<SearchEvidence> evidenceList = new ArrayList<>();
        
        try {
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode webPages = rootNode.path("webPages").path("value");
            
            if (!webPages.isArray() || webPages.size() == 0) {
                return evidenceList;
            }
            
            for (int i = 0; i < Math.min(webPages.size(), 5); i++) {
                JsonNode page = webPages.get(i);
                String title = page.path("name").asText();
                String snippet = page.path("snippet").asText();
                String url = page.path("url").asText();
                
                // 计算相关性得分（简单实现）
                double relevanceScore = calculateRelevanceScore(title, snippet, originalQuery);
                
                SearchEvidence evidence = new SearchEvidence(
                    title, 
                    snippet, 
                    url, 
                    relevanceScore, 
                    sourceType, 
                    originalQuery
                );
                
                evidenceList.add(evidence);
            }
            
        } catch (Exception ignored) {
            // 解析失败时返回空列表
        }
        
        return evidenceList;
    }
    
    /**
     * 计算搜索结果的相关性得分
     */
    private double calculateRelevanceScore(String title, String snippet, String query) {
        String combinedText = (title + " " + snippet).toLowerCase();
        String lowerQuery = query.toLowerCase();
        
        double score = 0.0;
        
        // 标题匹配权重更高
        if (combinedText.contains(lowerQuery)) {
            score += 0.5;
        }
        
        // 关键词匹配
        String[] queryWords = lowerQuery.split("\\s+");
        for (String word : queryWords) {
            if (word.length() > 1 && combinedText.contains(word)) {
                score += 0.1;
            }
        }
        
        return Math.min(score, 1.0); // 最高得分为1.0
    }
    
    /**
     * 将 SearchEvidence 列表格式化为结构化的字符串
     */
    private String formatSearchEvidenceAsString(List<SearchEvidence> evidences, String originalQuery) {
        if (evidences == null || evidences.isEmpty()) {
            return "未找到与 \"" + originalQuery + "\" 相关的医疗信息。";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("🔍 基于 \"").append(originalQuery).append("\" 的医疗搜索结果：\n\n");
        
        for (int i = 0; i < evidences.size(); i++) {
            SearchEvidence evidence = evidences.get(i);
            result.append("📋 证据").append(i + 1).append(" [").append(evidence.getSourceType()).append("]\n");
            result.append("标题: ").append(evidence.getTitle()).append("\n");
            result.append("摘要: ").append(evidence.getSnippet()).append("\n");
            result.append("来源: ").append(evidence.getUrl()).append("\n");
            result.append("相关性得分: ").append(String.format("%.2f", evidence.getRelevanceScore())).append("\n\n");
        }
        
        result.append("💡 请基于以上搜索结果，结合医疗专业知识，为用户提供专业的分导诊建议。");
        
        return result.toString();
    }
    
}
