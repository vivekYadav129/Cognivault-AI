package com.example.aiknowledgeassistant.dto.response;

import java.util.List;

public class SearchResponse {
    private String query;
    private List<SearchResultDto> results;
    private int count;

    public SearchResponse() {
    }

    public SearchResponse(String query, List<SearchResultDto> results) {
        this.query = query;
        this.results = results;
        this.count = results != null ? results.size() : 0;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public List<SearchResultDto> getResults() { return results; }
    public void setResults(List<SearchResultDto> results) {
        this.results = results;
        this.count = results != null ? results.size() : 0;
    }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
