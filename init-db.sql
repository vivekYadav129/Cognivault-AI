-- ============================================================
-- Cognivault AI — PostgreSQL 16 + pgvector Production Schema
-- ============================================================

-- 1. Enable the pgvector extension for high-performance vector indexing
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);

-- 3. Documents Table
CREATE TABLE IF NOT EXISTS documents (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    original_file_name VARCHAR(255) NOT NULL,
    stored_file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    file_size BIGINT NOT NULL,
    storage_path VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'UPLOADED',
    extracted_text TEXT,
    page_count INT,
    error_message VARCHAR(1000),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_documents_user_id ON documents(user_id);

-- 4. Document Chunks Table with Native Vector Support
CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGSERIAL PRIMARY KEY,
    document_id BIGINT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    character_count INT NOT NULL,
    embedding TEXT,
    embedding_vec vector(1536),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_document_chunks_doc_id ON document_chunks(document_id);
CREATE INDEX IF NOT EXISTS idx_document_chunks_index ON document_chunks(document_id, chunk_index);

-- 5. High-Performance HNSW Vector Cosine Index (sub-millisecond similarity retrieval)
CREATE INDEX IF NOT EXISTS idx_document_chunks_hnsw 
ON document_chunks USING hnsw (embedding_vec vector_cosine_ops);

-- 6. Trigger to automatically populate and synchronize native pgvector column from embedding string
CREATE OR REPLACE FUNCTION sync_document_chunk_vector()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.embedding IS NOT NULL AND NEW.embedding <> '' THEN
        BEGIN
            IF LEFT(TRIM(NEW.embedding), 1) = '[' THEN
                NEW.embedding_vec := NEW.embedding::vector;
            ELSE
                NEW.embedding_vec := ('[' || NEW.embedding || ']')::vector;
            END IF;
        EXCEPTION WHEN OTHERS THEN
            NEW.embedding_vec := NULL;
        END;
    ELSE
        NEW.embedding_vec := NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_sync_document_chunk_vector ON document_chunks;
CREATE TRIGGER trg_sync_document_chunk_vector
BEFORE INSERT OR UPDATE OF embedding ON document_chunks
FOR EACH ROW
EXECUTE FUNCTION sync_document_chunk_vector();

-- 7. Quizzes Table
CREATE TABLE IF NOT EXISTS quizzes (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    document_id BIGINT REFERENCES documents(id) ON DELETE SET NULL,
    mode VARCHAR(30) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    topic_focus VARCHAR(255),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_quizzes_user_id ON quizzes(user_id);
CREATE INDEX IF NOT EXISTS idx_quizzes_doc_id ON quizzes(document_id);

-- 8. Quiz Questions Table
CREATE TABLE IF NOT EXISTS quiz_questions (
    id BIGSERIAL PRIMARY KEY,
    quiz_id BIGINT NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    question_index INT NOT NULL,
    question_text TEXT NOT NULL,
    options_json TEXT NOT NULL,
    correct_answer_index INT NOT NULL,
    explanation TEXT NOT NULL,
    category VARCHAR(30) NOT NULL,
    difficulty VARCHAR(20) NOT NULL,
    grounding_type VARCHAR(30) NOT NULL,
    skill_tag VARCHAR(100),
    code_snippet TEXT,
    question_signature VARCHAR(64),
    source_citation VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_quiz_questions_quiz_id ON quiz_questions(quiz_id);
CREATE INDEX IF NOT EXISTS idx_quiz_questions_sig ON quiz_questions(question_signature);

-- 9. Quiz Attempts Table
CREATE TABLE IF NOT EXISTS quiz_attempts (
    id BIGSERIAL PRIMARY KEY,
    quiz_id BIGINT NOT NULL REFERENCES quizzes(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    score INT NOT NULL,
    total_questions INT NOT NULL,
    percentage DOUBLE PRECISION NOT NULL,
    user_answers_json TEXT,
    topic_breakdown_json TEXT,
    difficulty_breakdown_json TEXT,
    completed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_quiz_attempts_user_id ON quiz_attempts(user_id);
CREATE INDEX IF NOT EXISTS idx_quiz_attempts_quiz_id ON quiz_attempts(quiz_id);

-- 10. Verification Notice
DO $$
BEGIN
    RAISE NOTICE 'Cognivault AI: PostgreSQL 16 schema and pgvector HNSW index initialized successfully.';
END $$;
