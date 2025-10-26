package com.isearch.text2vectorApp.controller;

import com.isearch.text2vectorApp.model.DocumentRequest;
import com.isearch.text2vectorApp.service.DocumentService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

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
}