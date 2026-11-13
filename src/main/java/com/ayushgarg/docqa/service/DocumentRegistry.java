package com.ayushgarg.docqa.service;

import com.ayushgarg.docqa.dto.DocumentInfo;
import com.ayushgarg.docqa.exception.UpstreamException;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

/**
 * Tiny registry of uploaded documents, kept in a plain JDBC table in the
 * same PostgreSQL database as the vector store. PgVector itself only stores
 * chunks, so this table remembers which file each upload was and when it
 * arrived (used by GET /api/documents).
 *
 * The table is created lazily on first use, so booting the app (and the
 * /api/health endpoint) never depends on the database being reachable.
 */
@Service
public class DocumentRegistry {

    private final JdbcTemplate jdbc;
    private volatile boolean tableReady;

    public DocumentRegistry(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private void ensureTable() {
        if (tableReady) {
            return;
        }
        synchronized (this) {
            if (tableReady) {
                return;
            }
            jdbc.execute("""
                    CREATE TABLE IF NOT EXISTS documents (
                        id          VARCHAR(64)  PRIMARY KEY,
                        file_name   VARCHAR(512) NOT NULL,
                        chunks      INTEGER      NOT NULL,
                        uploaded_at TIMESTAMPTZ  NOT NULL DEFAULT now()
                    )
                    """);
            tableReady = true;
        }
    }

    public void save(String id, String fileName, int chunks) {
        try {
            ensureTable();
            jdbc.update("INSERT INTO documents (id, file_name, chunks) VALUES (?, ?, ?)",
                    id, fileName, chunks);
        } catch (DataAccessException ex) {
            throw new UpstreamException("Database unavailable while saving the document record.", ex);
        }
    }

    public List<DocumentInfo> findAll() {
        try {
            ensureTable();
            return jdbc.query(
                    "SELECT id, file_name, chunks, uploaded_at FROM documents ORDER BY uploaded_at DESC",
                    (rs, rowNum) -> new DocumentInfo(
                            rs.getString("id"),
                            rs.getString("file_name"),
                            rs.getInt("chunks"),
                            rs.getObject("uploaded_at", Timestamp.class).toInstant()));
        } catch (DataAccessException ex) {
            throw new UpstreamException("Database unavailable while listing documents.", ex);
        }
    }
}
