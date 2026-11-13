package com.ayushgarg.docqa.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared AI beans.
 *
 * The {@link ChatClient} talks to whichever OpenAI-compatible chat API is
 * configured (see OPENAI_BASE_URL / AI_MODEL), e.g. Groq's free tier.
 * Embeddings are NOT configured here: Spring AI's Transformers starter
 * provides a local ONNX EmbeddingModel automatically.
 */
@Configuration
public class AppConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    /**
     * Splits documents into token-sized chunks suitable for embedding
     * and retrieval (roughly 512 tokens per chunk with sensible bounds).
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return TokenTextSplitter.builder()
                .withChunkSize(512)
                .withMinChunkSizeChars(200)
                .withMinChunkLengthToEmbed(10)
                .withMaxNumChunks(10_000)
                .withKeepSeparator(true)
                .build();
    }
}
