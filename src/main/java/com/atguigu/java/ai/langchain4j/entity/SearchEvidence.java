package com.atguigu.java.ai.langchain4j.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 搜索结果证据类
 * 用于存储从搜索引擎获取的结构化信息
 */
public class SearchEvidence {
    
    @JsonProperty("title")
    private String title;
    
    @JsonProperty("snippet")
    private String snippet;
    
    @JsonProperty("url")
    private String url;
    
    @JsonProperty("relevance_score")
    private Double relevanceScore;
    
    @JsonProperty("source_type")
    private String sourceType; // 如：medical_website, hospital_info, department_info
    
    @JsonProperty("search_query")
    private String searchQuery;
    
    public SearchEvidence() {}
    
    public SearchEvidence(String title, String snippet, String url, Double relevanceScore, String sourceType, String searchQuery) {
        this.title = title;
        this.snippet = snippet;
        this.url = url;
        this.relevanceScore = relevanceScore;
        this.sourceType = sourceType;
        this.searchQuery = searchQuery;
    }
    
    // Getters and Setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getSnippet() {
        return snippet;
    }
    
    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public Double getRelevanceScore() {
        return relevanceScore;
    }
    
    public void setRelevanceScore(Double relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
    
    public String getSourceType() {
        return sourceType;
    }
    
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
    
    public String getSearchQuery() {
        return searchQuery;
    }
    
    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }
    
    @Override
    public String toString() {
        return "SearchEvidence{" +
                "title='" + title + '\'' +
                ", snippet='" + snippet + '\'' +
                ", url='" + url + '\'' +
                ", relevanceScore=" + relevanceScore +
                ", sourceType='" + sourceType + '\'' +
                ", searchQuery='" + searchQuery + '\'' +
                '}';
    }
}
