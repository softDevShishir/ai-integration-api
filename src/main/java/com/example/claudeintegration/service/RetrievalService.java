package com.example.claudeintegration.service;

import com.example.claudeintegration.dto.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Retrieval step of RAG: finds the products most relevant to a question using
 * PostgreSQL full-text search over name + description + category.
 */
@Service
public class RetrievalService {

    static final int MAX_RESULTS = 5;

    /*
     * plainto_tsquery() turns free text into lexemes joined with AND ('red' & 'shoe'),
     * which would require every word of a natural-language question to match. Swapping
     * '&' for '|' makes any matching word count, and ts_rank puts products matching
     * more (and rarer) words first. search_vector is a generated column holding
     * to_tsvector('english', name || ' ' || description || ' ' || category); see schema.sql.
     */
    private static final String SEARCH_SQL = """
            SELECT id, name, description, category, price, in_stock
            FROM products,
                 to_tsquery('english', replace(plainto_tsquery('english', ?)::text, '&', '|')) AS query
            WHERE search_vector @@ query
            ORDER BY ts_rank(search_vector, query) DESC, id
            LIMIT ?
            """;

    private static final RowMapper<Product> PRODUCT_ROW_MAPPER = (rs, rowNum) -> new Product(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("category"),
            rs.getBigDecimal("price"),
            rs.getBoolean("in_stock"));

    private final JdbcTemplate jdbcTemplate;

    public RetrievalService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Returns up to {@value #MAX_RESULTS} products matching the question, best
     * match first, or an empty list if nothing matches.
     */
    public List<Product> search(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        return jdbcTemplate.query(SEARCH_SQL, PRODUCT_ROW_MAPPER, question, MAX_RESULTS);
    }
}
