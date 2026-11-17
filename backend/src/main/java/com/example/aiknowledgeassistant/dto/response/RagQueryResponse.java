package com.example.aiknowledgeassistant.dto.response;

import java.util.List;

public class RagQueryResponse {
    private String answer;
    private List<CitationDto> citations;
    private int sourcesUsed;

    public RagQueryResponse() {
    }

    public RagQueryResponse(String answer, List<CitationDto> citations, int sourcesUsed) {
        this.answer = answer;
        this.citations = citations;
        this.sourcesUsed = sourcesUsed;
    }

    public static RagQueryResponse noContext() {
        return new RagQueryResponse(
                "I couldn't find enough information in your uploaded documents to answer this question.",
                List.of(),
                0
        );
    }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public List<CitationDto> getCitations() { return citations; }
    public void setCitations(List<CitationDto> citations) { this.citations = citations; }

    public int getSourcesUsed() { return sourcesUsed; }
    public void setSourcesUsed(int sourcesUsed) { this.sourcesUsed = sourcesUsed; }
}
