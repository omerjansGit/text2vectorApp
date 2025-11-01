package com.isearch.text2vectorApp.util;

import org.apache.tika.Tika;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for detecting and validating file types using Apache Tika.
 * Tika provides robust file type detection based on file content (magic bytes)
 * and filename extensions.
 */
public class FileTypeDetector {

    private static final Tika TIKA = new Tika();
    private static final Set<String> SUPPORTED_FILE_TYPES = Set.of("pdf", "docx", "txt");

    // MIME type to file extension mapping
    private static final Map<String, String> MIME_TO_EXTENSION = Map.of(
            "application/pdf", "pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx",
            "text/plain", "txt"
    );

    /**
     * Detects the file type from a MultipartFile using Tika.
     *
     * @param file the uploaded file
     * @return the file type (pdf, docx, txt) in lowercase
     * @throws IllegalArgumentException if file type is not supported
     */
    public static String detectFileType(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is null or empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename is null or empty");
        }

        try {
            // Try Tika detection first (content-based)
            String mimeType = TIKA.detect(file.getInputStream(), filename);
            String fileType = MIME_TO_EXTENSION.get(mimeType);

            if (fileType != null) {
                return fileType;
            }
        } catch (IOException e) {
            // Fall through to extension-based detection
        }

        // Fallback to extension-based detection
        String extension = getFileExtension(filename);
        if (extension == null) {
            throw new IllegalArgumentException("Cannot determine file type for: " + filename);
        }

        String lowerExtension = extension.toLowerCase();
        if (!SUPPORTED_FILE_TYPES.contains(lowerExtension)) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + extension + ". Supported types: " + SUPPORTED_FILE_TYPES);
        }

        return lowerExtension;
    }

    /**
     * Detects the file type from a Resource using Tika.
     *
     * @param resource the resource
     * @return the file type (pdf, docx, txt) in lowercase
     * @throws IllegalArgumentException if file type is not supported
     */
    public static String detectFileType(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Resource is null");
        }

        String filename = resource.getFilename();
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Resource filename is null or empty");
        }

        try {
            // Try Tika detection (content-based)
            String mimeType = TIKA.detect(resource.getInputStream(), filename);
            String fileType = MIME_TO_EXTENSION.get(mimeType);

            if (fileType != null) {
                return fileType;
            }
        } catch (IOException e) {
            // Fall through to extension-based detection
        }

        // Fallback to extension-based detection
        String extension = getFileExtension(filename);
        if (extension == null) {
            throw new IllegalArgumentException("Cannot determine file type for: " + filename);
        }

        String lowerExtension = extension.toLowerCase();
        if (!SUPPORTED_FILE_TYPES.contains(lowerExtension)) {
            throw new IllegalArgumentException(
                    "Unsupported file type: " + extension + ". Supported types: " + SUPPORTED_FILE_TYPES);
        }

        return lowerExtension;
    }

    /**
     * Validates that a file type is supported.
     *
     * @param fileType the file type to validate
     * @return true if supported, false otherwise
     */
    public static boolean isSupported(String fileType) {
        return fileType != null && SUPPORTED_FILE_TYPES.contains(fileType.toLowerCase());
    }

    /**
     * Extracts the file extension from a filename.
     *
     * @param filename the filename
     * @return the extension without the dot, or null if no extension found
     */
    private static String getFileExtension(String filename) {
        if (filename == null) {
            return null;
        }

        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return null;
        }

        return filename.substring(lastDotIndex + 1);
    }
}
