
package com.example.aiknowledgeassistant;

import com.example.aiknowledgeassistant.service.DocumentService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        name = "cognivault.migration.reembed-existing",
        havingValue = "true"
)
public class EmbeddingMigrationRunner implements CommandLineRunner {

    private final DocumentService documentService;

    public EmbeddingMigrationRunner(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public void run(String... args) {
        int count = documentService.reembedExistingDocument(1L);
        System.out.println("Gemini migration completed. Updated chunks: " + count);
    }
}
