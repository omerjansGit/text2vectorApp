package com.isearch.text2vectorApp.service;

import com.isearch.text2vectorApp.exception.EmbeddingServiceException;
import com.isearch.text2vectorApp.util.CustomPagedPdfDocumentReader;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.*;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentService {

    private final EmbeddingModel model;
    private final CustomPagedPdfDocumentReader pdfReader;

    public DocumentService(EmbeddingModel model, CustomPagedPdfDocumentReader pdfReader) {
        this.model = model;
        this.pdfReader = pdfReader;
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

    public List<List<float[]>> generateEmbeddingsFromPdfs(List<Resource> pdfResources) {
        return pdfResources.stream()
                .map(this::generateEmbeddingsFromPdf)
                .toList();
    }

    public List<float[]> generateEmbeddingsFromPdf(Resource pdfResource) {
        try {
            List<Document> documents = pdfReader.getDocsFromPdf(pdfResource);

            if (documents.isEmpty()) {
                throw new EmbeddingServiceException("Could not extract text from the PDF.");
            }

            EmbeddingOptions embeddingOptions = EmbeddingOptionsBuilder.builder().build();
            BatchingStrategy batchingStrategy = new TokenCountBatchingStrategy();

            return model.embed(documents, embeddingOptions, batchingStrategy);

        } catch (Exception ex) {
            throw new EmbeddingServiceException("Error generating PDF embeddings: " + ex.getMessage(), ex);
        }
    }
}
