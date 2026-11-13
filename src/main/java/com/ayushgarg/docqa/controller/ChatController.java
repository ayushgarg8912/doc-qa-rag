package com.ayushgarg.docqa.controller;

import com.ayushgarg.docqa.dto.AskRequest;
import com.ayushgarg.docqa.dto.AskResponse;
import com.ayushgarg.docqa.service.ChatService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Ask questions answered from the uploaded documents (RAG). */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Body: {"question": "..."}
     * Returns: {"answer": "...", "sources": ["file1.pdf", ...]}
     */
    @PostMapping("/ask")
    public AskResponse ask(@RequestBody AskRequest request) {
        return chatService.ask(request == null ? null : request.question());
    }
}
