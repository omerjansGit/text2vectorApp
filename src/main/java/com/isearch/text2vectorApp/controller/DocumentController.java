package com.isearch.text2vectorApp.controller;

import com.isearch.text2vectorApp.exception.EmbeddingServiceException;
import com.isearch.text2vectorApp.model.DocumentEmbeddingResponse;
import com.isearch.text2vectorApp.model.DocumentRequest;
import com.isearch.text2vectorApp.service.DocumentService;
import com.isearch.text2vectorApp.util.DocumentReaderFactory;
import com.isearch.text2vectorApp.util.FileTypeDetector;
import com.isearch.text2vectorApp.util.ResourceUtils;
import jakarta.validation.Valid;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1")
public class DocumentController {

    private final EmbeddingModel embeddingModel;
    private final DocumentService documentService;
    private final VectorStore vectorStore;
    private final DocumentReaderFactory documentReaderFactory;

    @Autowired
    public DocumentController(EmbeddingModel embeddingModel, DocumentService documentService, VectorStore vectorStore, DocumentReaderFactory documentReaderFactory) {
        this.embeddingModel = embeddingModel;
        this.documentService = documentService;
        this.vectorStore = vectorStore;
        this.documentReaderFactory = documentReaderFactory;
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

    // ========== Unified endpoints for all file types ==========

    /**
     * Unified endpoint for embedding a single document of any supported type (PDF, DOCX, TXT).
     * The file type is automatically detected.
     *
     * @param file the document file to embed
     * @return response with embeddings
     */
    @PostMapping(value = "/embed/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> embedDocument(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            String fileType = FileTypeDetector.detectFileType(file);
            Resource resource = ResourceUtils.toResource(file);
            List<float[]> embeddings = documentService.generateEmbeddingsFromDocument(resource);

            return ResponseEntity.ok(new DocumentEmbeddingResponse(
                    file.getOriginalFilename(),
                    fileType,
                    embeddings.size(),
                    embeddings));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("Invalid file: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Error embedding document: " + ex.getMessage());
        }
    }

    /**
     * Unified endpoint for embedding multiple documents of any supported types (PDF, DOCX, TXT).
     * Files can be mixed types - the system will automatically detect and process each one.
     *
     * @param files list of document files to embed
     * @return response with embeddings for all documents
     */
    @PostMapping(value = "/embed/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> embedDocuments(@RequestParam("files") List<MultipartFile> files) {
        if (files.isEmpty()) {
            throw new EmbeddingServiceException("File list is empty");
        }

        try {
            // Convert uploaded files to Spring Resources and detect file types
            List<Resource> resources = new ArrayList<>();
            List<String> fileTypes = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    throw new EmbeddingServiceException("One or more files are empty");
                }
                fileTypes.add(FileTypeDetector.detectFileType(file));
                resources.add(ResourceUtils.toResource(file));
            }

            // Generate embeddings for all documents
            List<List<float[]>> allEmbeddings = documentService.generateEmbeddingsFromDocuments(resources);

            // Build results
            List<Map<String, Object>> results = new ArrayList<>();
            for (int i = 0; i < resources.size(); i++) {
                Resource res = resources.get(i);
                results.add(Map.of(
                        "filename", Objects.requireNonNull(res.getFilename()),
                        "fileType", fileTypes.get(i),
                        "chunks", allEmbeddings.get(i).size(),
                        "vectors", allEmbeddings.get(i)
                ));
            }

            return Map.of("files", results);

        } catch (IllegalArgumentException ex) {
            throw new EmbeddingServiceException("Invalid file: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new EmbeddingServiceException("Error embedding documents: " + ex.getMessage(), ex);
        }
    }

    // ========== Vector Store Endpoints ==========

    /**
     * Store a document in the vector database.
     * VectorStore automatically generates embeddings and stores both the vector and text.
     *
     * @param file the document file to store
     * @return response with storage confirmation
     */
    @PostMapping(value = "/store/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> storeDocument(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File is empty");
            }

            String fileType = FileTypeDetector.detectFileType(file);
            Resource resource = ResourceUtils.toResource(file);

            // Read documents from file
            var reader = documentReaderFactory.getReader(resource);
            List<Document> documents = reader.read(resource);

            if (documents.isEmpty()) {
                return ResponseEntity.badRequest().body("Could not extract text from the document");
            }

            // Add metadata to documents
            String filename = file.getOriginalFilename();
            for (Document doc : documents) {
                doc.getMetadata().put("source", filename != null ? filename : "unknown");
                doc.getMetadata().put("fileType", fileType);
            }

            // Store in vector database (automatically generates embeddings)
            vectorStore.add(documents);

            return ResponseEntity.ok(Map.of(
                    "message", "Document stored successfully",
                    "filename", filename != null ? filename : "unknown",
                    "fileType", fileType,
                    "chunks", documents.size()
            ));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body("Invalid file: " + ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).body("Error storing document: " + ex.getMessage());
        }
    }

    /**
     * Store multiple documents in the vector database.
     *
     * @param files list of document files to store
     * @return response with storage confirmation
     */
    @PostMapping(value = "/store/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> storeDocuments(@RequestParam("files") List<MultipartFile> files) {
        if (files.isEmpty()) {
            throw new EmbeddingServiceException("File list is empty");
        }

        try {
            int totalChunks = 0;
            List<Map<String, Object>> results = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    throw new EmbeddingServiceException("One or more files are empty");
                }

                String fileType = FileTypeDetector.detectFileType(file);
                Resource resource = ResourceUtils.toResource(file);

                // Read documents from file
                var reader = documentReaderFactory.getReader(resource);
                List<Document> documents = reader.read(resource);

                if (!documents.isEmpty()) {
                    // Add metadata
                    String filename = file.getOriginalFilename();
                    for (Document doc : documents) {
                        doc.getMetadata().put("source", filename != null ? filename : "unknown");
                        doc.getMetadata().put("fileType", fileType);
                    }

                    // Store in vector database (automatically generates embeddings)
                    vectorStore.add(documents);
                    totalChunks += documents.size();

                    results.add(Map.of(
                            "filename", filename != null ? filename : "unknown",
                            "fileType", fileType,
                            "chunks", documents.size(),
                            "status", "stored"
                    ));
                }
            }

            return Map.of(
                    "message", "Documents stored successfully",
                    "totalChunks", totalChunks,
                    "files", results
            );

        } catch (Exception ex) {
            throw new EmbeddingServiceException("Error storing documents: " + ex.getMessage(), ex);
        }
    }

    /**
     * Search for similar documents in the vector database.
     *
     * @param query the search query text
     * @param topK  number of results to return (default: 3)
     * @return list of similar documents with their content and metadata
     */
    @GetMapping("/search")
    public List<Document> searchDocuments(
            @RequestParam("query") String query,
            @RequestParam(value = "topK", defaultValue = "3") int topK) {

        try {
            return vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(topK)
                            .build())
                    .stream()
                    //.map(Document::getFormattedContent)
                    .toList();

            /*List<Map<String, Object>> documents = results.stream()
                    .map(doc -> {
                        doc.getId();
                        return Map.of(
                                "content", doc.getFormattedContent(),
                                "metadata", doc.getMetadata(),
                                "id", doc.getId()
                        );
                    })
                    .toList();

            return Map.of(
                    "query", query,
                    "resultsCount", results.size(),
                    "documents", documents
            ); */

        } catch (Exception ex) {
            throw new EmbeddingServiceException("Error searching documents: " + ex.getMessage(), ex);
        }
    }

}