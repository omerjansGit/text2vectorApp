package com.isearch.text2vectorApp.util;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Document reader implementation for TXT (plain text) files.
 */
@Component
public class TxtDocumentReader implements DocumentReader {
    @Override
    public List<Document> read(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).trim();

            if (content.isEmpty()) {
                return List.of();
            }

            // Create a single document with all content
            var doc = new Document(content);
            String filename = resource.getFilename();
            doc.getMetadata().put("source", filename != null ? filename : "unknown");
            doc.getMetadata().put("fileType", "txt");

            return List.of(doc);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read TXT file: " + resource.getFilename(), e);
        }
    }
}
