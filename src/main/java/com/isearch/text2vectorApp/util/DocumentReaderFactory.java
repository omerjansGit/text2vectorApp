package com.isearch.text2vectorApp.util;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Factory for creating appropriate DocumentReader instances based on file type.
 */
@Component
public class DocumentReaderFactory {

    private final Map<String, DocumentReader> readers;

    public DocumentReaderFactory(PdfDocumentReader pdfReader,
                                 DocxDocumentReader docxReader,
                                 TxtDocumentReader txtReader) {
        this.readers = Map.of(
                "pdf", pdfReader,
                "docx", docxReader,
                "txt", txtReader
        );
    }

    /**
     * Gets the appropriate DocumentReader for a given file type.
     *
     * @param fileType the file type (pdf, docx, txt)
     * @return the DocumentReader implementation
     * @throws IllegalArgumentException if file type is not supported
     */
    public DocumentReader getReader(String fileType) {
        if (fileType == null) {
            throw new IllegalArgumentException("File type cannot be null");
        }

        String lowerFileType = fileType.toLowerCase();
        DocumentReader reader = readers.get(lowerFileType);

        if (reader == null) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + fileType + ". Supported types: " + readers.keySet());
        }

        return reader;
    }

    /**
     * Gets the appropriate DocumentReader for a MultipartFile by detecting its type.
     *
     * @param file the uploaded file
     * @return the DocumentReader implementation
     */
    public DocumentReader getReader(MultipartFile file) {
        String fileType = FileTypeDetector.detectFileType(file);
        return getReader(fileType);
    }

    /**
     * Gets the appropriate DocumentReader for a Resource by detecting its type.
     *
     * @param resource the resource
     * @return the DocumentReader implementation
     */
    public DocumentReader getReader(Resource resource) {
        String fileType = FileTypeDetector.detectFileType(resource);
        return getReader(fileType);
    }
}


