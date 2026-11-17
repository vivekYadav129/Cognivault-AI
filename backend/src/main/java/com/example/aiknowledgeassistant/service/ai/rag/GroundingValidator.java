package com.example.aiknowledgeassistant.service.ai.rag;

import com.example.aiknowledgeassistant.dto.response.CitationDto;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GroundingValidator {

    public boolean isGrounded(String answer, List<CitationDto> citations, boolean hasContext) {
        if (!hasContext) {
            return false;
        }

        if (answer == null || answer.isBlank()) {
            return false;
        }

        // If the answer states that information couldn't be found, it is considered grounded
        if (answer.toLowerCase().contains("couldn't find enough information") ||
            answer.toLowerCase().contains("could not find enough information")) {
            return true;
        }

        return citations != null && !citations.isEmpty();
    }
}
