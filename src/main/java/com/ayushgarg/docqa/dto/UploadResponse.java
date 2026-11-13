package com.ayushgarg.docqa.dto;

/** Response after a document has been parsed, chunked and embedded. */
public record UploadResponse(String documentId, String fileName, int chunks) {
}
