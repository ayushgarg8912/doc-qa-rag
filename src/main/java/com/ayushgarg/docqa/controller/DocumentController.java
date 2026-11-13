package com.ayushgarg.docqa.controller;

import com.ayushgarg.docqa.dto.DocumentInfo;
import com.ayushgarg.docqa.dto.UploadResponse;
import com.ayushgarg.docqa.service.DocumentService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Document ingestion + listing endpoints. */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /**
     * Upload a .pdf, .txt or .md file. The file is parsed, split into
     * chunks, embedded and stored in the vector store.
     *
     * @return {documentId, fileName, chunks}
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        return documentService.ingest(file);
    }

    /** List all previously uploaded documents. */
    @GetMapping
    public List<DocumentInfo> list() {
        return documentService.listDocuments();
    }
}
