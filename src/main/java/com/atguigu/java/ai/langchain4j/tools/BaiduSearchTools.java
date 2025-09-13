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
import reactor.core.publisher.Mono;

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
