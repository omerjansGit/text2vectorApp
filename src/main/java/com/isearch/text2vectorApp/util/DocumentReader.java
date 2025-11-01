package com.isearch.text2vectorApp.util;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;

import java.util.List;

/**
 * Interface for reading documents from various file formats.
 * Implementations should handle specific file types (PDF, DOCX, TXT, etc.)
 */
public interface DocumentReader {

    /**
     * Reads a document from a resource and returns a list of Spring AI Documents.
     * For multi-page documents (like PDFs), each page may be a separate Document.
     * For single-page documents (like TXT), typically one Document is returned.
     *
     * @param resource the resource to read from
     * @return list of Document objects containing the extracted text and metadata
     */
    List<Document> read(Resource resource);
}
