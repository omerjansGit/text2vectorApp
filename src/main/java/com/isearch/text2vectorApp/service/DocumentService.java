package com.isearch.text2vectorApp.service;

import com.isearch.text2vectorApp.exception.EmbeddingServiceException;
import com.isearch.text2vectorApp.util.DocumentReaderFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingOptionsBuilder;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for generating embeddings from text and various document types.
 * Supports PDF, DOCX, and TXT files.
 */
@Service
@Slf4j
public class DocumentService {

    private final EmbeddingModel model;
    private final DocumentReaderFactory documentReaderFactory;
    private final TokenCountEstimator tokenCountEstimator;


    public DocumentService(EmbeddingModel model,
                           DocumentReaderFactory documentReaderFactory, TokenCountEstimator tokenCountEstimator) {
        this.model = model;
        this.documentReaderFactory = documentReaderFactory;
        this.tokenCountEstimator = tokenCountEstimator;
    }

    /**
     * Generates embedding from a list of text strings.
     *
     * @param texts list of text strings
     * @return embedding vector
     */
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

    /**
     * Generates embeddings from a single document of any supported type.
     *
     * @param resource the document resource
     * @return list of embedding vectors (one per document chunk)
     */
    public List<float[]> generateEmbeddingsFromDocument(Resource resource) {
        try {
            var reader = documentReaderFactory.getReader(resource);
            List<Document> documents = reader.read(resource);

            if (documents.isEmpty()) {
                throw new EmbeddingServiceException(
                        "Could not extract text from the document: " + resource.getFilename());
            }

            // Calculate accurate token counts using Spring AI's TokenCountEstimator
            int totalTokens = 0;
            int maxChunkTokens = 0;
            for (Document doc : documents) {
                int chunkTokens = tokenCountEstimator.estimate(doc.getText());
                totalTokens += chunkTokens;
                maxChunkTokens = Math.max(maxChunkTokens, chunkTokens);
            }

            String filename = resource.getFilename() != null ? resource.getFilename() : "unknown";
            log.info("Document: {} | Chunks: {} | Total tokens: {} | Max chunk tokens: {} | Context limit: 8192",
                    filename, documents.size(), totalTokens, maxChunkTokens);

            if (maxChunkTokens > 8192) {
                log.warn("WARNING: Document '{}' has chunk with {} tokens, exceeding context limit of 8192. May cause EOF errors.", filename, maxChunkTokens);
            }
            if (totalTokens > 8192 && maxChunkTokens <= 8192) {
                log.info("INFO: Document '{}' has total {} tokens. Batching strategy will split into multiple requests.", filename, totalTokens);
            }

            var embeddingOptions = EmbeddingOptionsBuilder.builder().build();
            var batchingStrategy = new TokenCountBatchingStrategy();

            return model.embed(documents, embeddingOptions, batchingStrategy);

        } catch (EmbeddingServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new EmbeddingServiceException(
                    "Error generating document embeddings: " + ex.getMessage(), ex);
        }
    }

    /**
     * Generates embeddings from multiple documents of any supported type.
     *
     * @param resources list of document resources
     * @return list of embedding lists (one list per document)
     */
    public List<List<float[]>> generateEmbeddingsFromDocuments(List<Resource> resources) {
        return resources.stream()
                .map(this::generateEmbeddingsFromDocument)
                .toList();
    }

}
