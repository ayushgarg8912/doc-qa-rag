package com.ayushgarg.docqa.dto;

/** Clean JSON error body returned for every API failure (never a stack trace). */
public record ErrorResponse(String error) {
}
