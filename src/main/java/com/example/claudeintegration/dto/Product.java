package com.example.claudeintegration.dto;

import java.math.BigDecimal;

/**
 * A row of the {@code products} table, used as retrieval context for RAG.
 * {@code price} is in BDT.
 */
public record Product(
        long id,
        String name,
        String description,
        String category,
        BigDecimal price,
        boolean inStock
) {
}
