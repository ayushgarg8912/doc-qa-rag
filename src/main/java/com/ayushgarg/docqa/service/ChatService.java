package com.ayushgarg.docqa.service;

import com.ayushgarg.docqa.dto.AskResponse;
import com.ayushgarg.docqa.exception.UpstreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The RAG query pipeline: embed the question, retrieve the most similar
 * chunks from PgVector, then ask the chat model to answer using ONLY that
 * retrieved context.
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final int TOP_K = 4;
    private static final double SIMILARITY_THRESHOLD = 0.30;

    private static final String SYSTEM_PROMPT = """
            You are a document question-answering assistant.
            Answer the user's question using ONLY the context passages provided in the user message.
            Never use outside knowledge and never invent facts.
            If the context does not contain the answer, say clearly that you could not find it
            in the uploaded documents.
            Keep the answer clear, accurate and concise.
            """;

    static final String NOT_FOUND_ANSWER =
            "I couldn't find anything about that in the uploaded documents. "
                    + "Please upload a document that covers the topic, or try rephrasing the question.";

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public ChatService(VectorStore vectorStore, ChatClient chatClient) {
        this.vectorStore = vectorStore;
        this.chatClient = chatClient;
    }

    public AskResponse ask(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("Question must not be empty.");
        }

        List<Document> retrieved;
        try {
            retrieved = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(question.trim())
                    .topK(TOP_K)
                    .similarityThreshold(SIMILARITY_THRESHOLD)
                    .build());
        } catch (Exception ex) {
            log.error("Similarity search failed", ex);
            throw new UpstreamException(
                    "Search failed: the vector database or embedding model is unavailable.", ex);
        }

        if (retrieved == null || retrieved.isEmpty()) {
            return new AskResponse(NOT_FOUND_ANSWER, List.of());
        }

        String context = retrieved.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        String userPrompt = """
                Context from the uploaded documents:
                %s

                Question: %s

                Answer using only the context above. If the context does not contain
                the answer, say so clearly instead of guessing.
                """.formatted(context, question.trim());

        String answer;
        try {
            answer = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception ex) {
            log.error("Chat model call failed", ex);
            throw new UpstreamException(
                    "The AI chat service is unavailable or rejected the request "
                            + "(check OPENAI_API_KEY / OPENAI_BASE_URL / AI_MODEL).", ex);
        }

        List<String> sources = retrieved.stream()
                .map(d -> d.getMetadata().get("fileName"))
                .filter(v -> v != null && !v.toString().isBlank())
                .map(Object::toString)
                .distinct()
                .toList();

        return new AskResponse(
                (answer == null || answer.isBlank()) ? NOT_FOUND_ANSWER : answer.trim(),
                sources);
    }
}
