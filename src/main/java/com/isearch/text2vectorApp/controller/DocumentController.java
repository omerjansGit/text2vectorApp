package com.isearch.text2vectorApp.controller;

import com.isearch.text2vectorApp.exception.EmbeddingServiceException;
import com.isearch.text2vectorApp.model.DocumentRequest;
import com.isearch.text2vectorApp.model.PdfEmbeddingResponse;
import com.isearch.text2vectorApp.service.DocumentService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final EmbeddingModel embeddingModel;
    private final DocumentService documentService;

    @Autowired
    public DocumentController(EmbeddingModel embeddingModel, DocumentService documentService) {
        this.embeddingModel = embeddingModel;
        this.documentService = documentService;
    }

    // Optional test endpoint
    @GetMapping("/embed")
    public Map<String, EmbeddingResponse> embed(@RequestParam(value = "text", defaultValue = "Random text") String text) {
        EmbeddingResponse response = this.embeddingModel.embedForResponse(List.of(text));
        return Map.of("Embedding", response);
    }

    // Main endpoint: POST JSON body {"texts":[list of texts]}
    @PostMapping("/embed")
    public Map<String, Object> getEmbedding(@Valid @RequestBody DocumentRequest request) {
        float[] vector = this.documentService.generateEmbedding(request.texts());
        return Map.of(
                "texts", request.texts(),
                "vector", vector,
                "length", vector.length
        );
    }

    // Embed single PDF
    @PostMapping(value = "/embed/pdf", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> embedPdf(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            ByteArrayResource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            List<float[]> embeddings = documentService.generateEmbeddingsFromPdf(resource);

            return ResponseEntity.ok().body(
                    new PdfEmbeddingResponse(file.getOriginalFilename(), embeddings.size(), embeddings)
            );

        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Error embedding PDF: " + ex.getMessage());
        }
    }

    // Embed multiple PDF files
    @PostMapping(value = "/embed/pdfs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> embedMultiplePdfs(@RequestParam("files") List<MultipartFile> files) {

        if (files.isEmpty()) {
            throw new EmbeddingServiceException("PDF file list is empty");
        }

        try {
            // Convert uploaded files to Spring Resources
            List<Resource> pdfResources = files.stream()
                    .map(file -> {
                        try {
                            return (Resource) new ByteArrayResource(file.getBytes()) {
                                @Override
                                public String getFilename() {
                                    return file.getOriginalFilename();
                                }
                            };
                        } catch (IOException e) {
                            throw new EmbeddingServiceException("Failed to read file: " + file.getOriginalFilename(), e);
                        }
                    })
                    .toList();

            // Delegate all embedding logic to the service
            List<List<float[]>> allEmbeddings = documentService.generateEmbeddingsFromPdfs(pdfResources);

            // Combine results (filename + vectors)
            List<Map<String, Object>> results = new ArrayList<>();
            for (int i = 0; i < pdfResources.size(); i++) {
                Resource res = pdfResources.get(i);
                List<float[]> vectors = allEmbeddings.get(i);
                results.add(Map.of(
                        "filename", Objects.requireNonNull(res.getFilename()),
                        "pages", vectors.size(),
                        "vectors", vectors
                ));
            }

            return Map.of("files", results);

        } catch (Exception ex) {
            throw new EmbeddingServiceException("Error embedding PDFs: " + ex.getMessage(), ex);
        }
    }
}