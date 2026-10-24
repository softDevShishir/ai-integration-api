-- Product catalogue used by the RAG endpoint (POST /api/ask/rag).
-- Runs on every startup (spring.sql.init.mode=always), so every statement is idempotent.

CREATE TABLE IF NOT EXISTS products (
    id            BIGINT PRIMARY KEY,
    name          VARCHAR(200)   NOT NULL,
    description   TEXT           NOT NULL,
    category      VARCHAR(50)    NOT NULL,
    price         NUMERIC(10, 2) NOT NULL,  -- in BDT
    in_stock      BOOLEAN        NOT NULL DEFAULT TRUE,
    -- Full-text search document over name + description + category, kept in sync by PostgreSQL.
    search_vector TSVECTOR GENERATED ALWAYS AS (
        to_tsvector('english', name || ' ' || description || ' ' || category)
    ) STORED
);

CREATE INDEX IF NOT EXISTS idx_products_search_vector ON products USING GIN (search_vector);
