package com.ayushgarg.docqa.dto;

import java.util.List;

/**
 * Response for POST /api/chat/ask: the grounded answer plus the file names
 * of the documents the answer was retrieved from.
 */
public record AskResponse(String answer, List<String> sources) {
}
