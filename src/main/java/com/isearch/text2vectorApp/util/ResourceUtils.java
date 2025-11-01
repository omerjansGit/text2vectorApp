package com.isearch.text2vectorApp.util;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Utility class for creating Spring Resource objects from MultipartFiles.
 * Reduces code duplication across controllers.
 */
public class ResourceUtils {

    /**
     * Converts a MultipartFile to a ByteArrayResource with the original filename preserved.
     *
     * @param file the multipart file
     * @return a Resource with the file's content and original filename
     * @throws IOException if file reading fails
     */
    public static Resource toResource(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is null or empty");
        }

        return new ByteArrayResource(file.getBytes()) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
    }
}

