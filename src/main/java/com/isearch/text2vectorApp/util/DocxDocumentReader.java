package com.isearch.text2vectorApp.util;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Document reader implementation for DOCX files using Apache POI.
 */
@Component
public class DocxDocumentReader implements DocumentReader {


    @Override
    public List<Document> read(Resource resource) {
        try (InputStream inputStream = resource.getInputStream();
             XWPFDocument document = new XWPFDocument(inputStream)) {

            StringBuilder text = new StringBuilder();

            // Extract text from all paragraphs
            document.getParagraphs().stream()
                    .map(XWPFParagraph::getText)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(p -> text.append(p).append("\n"));

            // Extract text from tables if any
            document.getTables().forEach(table ->
                    table.getRows().forEach(row -> {
                        row.getTableCells().stream()
                                .map(cell -> cell.getText().trim())
                                .filter(s -> !s.isEmpty())
                                .forEach(cell -> text.append(cell).append(" "));
                        text.append("\n");
                    }));

            String content = text.toString().trim();
            if (content.isEmpty()) {
                return List.of();
            }

            // Create a single document with metadata
            var doc = new Document(content);
            String filename = resource.getFilename();
            doc.getMetadata().put("source", filename != null ? filename : "unknown");
            doc.getMetadata().put("fileType", "docx");

            return List.of(doc);

        } catch (IOException e) {
            throw new RuntimeException("Failed to read DOCX file: " + resource.getFilename(), e);
        }
    }
}
