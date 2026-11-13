package com.ayushgarg.docqa.dto;

import java.time.Instant;

/** One uploaded document, as listed by GET /api/documents. */
public record DocumentInfo(String id, String fileName, int chunks, Instant uploadedAt) {
}
