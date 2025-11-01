package com.isearch.text2vectorApp.model;

import java.util.List;

/**
 * Response model for document embeddings.
 * Works for all file types (PDF, DOCX, TXT).
 */
public record DocumentEmbeddingResponse(String filename, String fileType, int chunks, List<float[]> vectors) {
}


