package com.isearch.text2vectorApp.service;

import com.isearch.text2vectorApp.exception.EmbeddingServiceException;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentService {

    private final EmbeddingModel model;

    public DocumentService(EmbeddingModel model) {
        this.model = model;
    }

    public float[] generateEmbedding(List<String> texts) {

        try {
            EmbeddingResponse response = model.embedForResponse(texts);

            if (response.getResults().isEmpty()) {
                throw new EmbeddingServiceException("Failed to generate embedding: response is empty.");
            }

            return response.getResults().getFirst().getOutput();
        } catch (Exception ex) {
            throw new EmbeddingServiceException("Error generating embedding: " + ex.getMessage(), ex);
        }
    }
}
