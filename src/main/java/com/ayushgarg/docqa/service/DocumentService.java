package com.ayushgarg.docqa.service;

import com.ayushgarg.docqa.dto.DocumentInfo;
import com.ayushgarg.docqa.dto.UploadResponse;
import com.ayushgarg.docqa.exception.UpstreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Ingestion pipeline: parse an uploaded file, split it into chunks, embed
 * the chunks with the configured embedding model API and store them in
 * PgVector with file-name / document-id metadata.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf", "txt", "md");

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;
    private final DocumentRegistry registry;

    public DocumentService(VectorStore vectorStore, TokenTextSplitter textSplitter,
                           DocumentRegistry registry) {
        this.vectorStore = vectorStore;
        this.textSplitter = textSplitter;
        this.registry = registry;
    }

    public UploadResponse ingest(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("No file uploaded. Attach a .pdf, .txt or .md file as form field 'file'.");
        }

        String fileName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = StringUtils.getFilenameExtension(fileName);
        if (fileName.isBlank() || extension == null
                || !SUPPORTED_EXTENSIONS.contains(extension.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException(
                    "Unsupported file type. Only .pdf, .txt and .md files are supported.");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Could not read the uploaded file.");
        }

        List<Document> parsed = extractText(bytes, fileName, extension.toLowerCase(Locale.ROOT));
        List<Document> chunks = textSplitter.apply(parsed);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "No readable text could be extracted from '" + fileName + "'.");
        }

        String documentId = UUID.randomUUID().toString();
        List<Document> enriched = chunks.stream().map(chunk -> {
            Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
            metadata.put("fileName", fileName);
            metadata.put("documentId", documentId);
            return new Document(chunk.getText(), metadata);
        }).toList();

        try {
            vectorStore.add(enriched);
        } catch (Exception ex) {
            log.error("Failed to embed/store document '{}'", fileName, ex);
            throw new UpstreamException(
                    "Failed to embed and store the document (vector database or embedding model error).", ex);
        }

        registry.save(documentId, fileName, enriched.size());
        log.info("Ingested '{}' as document {} ({} chunks)", fileName, documentId, enriched.size());
        return new UploadResponse(documentId, fileName, enriched.size());
    }

    public List<DocumentInfo> listDocuments() {
        return registry.findAll();
    }

    private List<Document> extractText(byte[] bytes, String fileName, String extension) {
        if ("pdf".equals(extension)) {
            try {
                ByteArrayResource resource = new ByteArrayResource(bytes) {
                    @Override
                    public String getFilename() {
                        return fileName;
                    }
                };
                List<Document> docs = new PagePdfDocumentReader(resource).get();
                boolean hasText = docs.stream()
                        .anyMatch(d -> d.getText() != null && !d.getText().isBlank());
                if (!hasText) {
                    throw new IllegalArgumentException(
                            "No readable text could be extracted from the PDF '" + fileName
                                    + "' (it may be scanned images only).");
                }
                return docs;
            } catch (IllegalArgumentException ex) {
                throw ex;
            } catch (Exception ex) {
                log.warn("PDF parsing failed for '{}': {}", fileName, ex.getMessage());
                throw new IllegalArgumentException(
                        "Could not parse '" + fileName + "' as a PDF file.");
            }
        }
        String text = new String(bytes, StandardCharsets.UTF_8);
        if (text.isBlank()) {
            throw new IllegalArgumentException("The file '" + fileName + "' is empty.");
        }
        return List.of(new Document(text));
    }
}
