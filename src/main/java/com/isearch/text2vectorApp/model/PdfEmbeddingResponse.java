package com.isearch.text2vectorApp.model;

import java.util.List;

public record PdfEmbeddingResponse(String filename, int pages, List<float[]> vectors) {
}
