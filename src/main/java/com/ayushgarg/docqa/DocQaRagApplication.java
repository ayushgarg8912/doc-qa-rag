package com.ayushgarg.docqa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Document Q&A with RAG and GenAI.
 *
 * Upload documents (PDF / TXT / MD), they are split into chunks, embedded
 * via a remote embedding model API and stored in PostgreSQL/pgvector.
 * Questions are answered by an LLM using only the retrieved document chunks
 * (Retrieval-Augmented Generation).
 */
@SpringBootApplication
public class DocQaRagApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocQaRagApplication.class, args);
    }
}
