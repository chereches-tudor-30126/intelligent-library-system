-- =============================================================================
-- LIBRARY SYSTEM - schema.sql
-- Database: PostgreSQL 15+
-- Description: Full schema for the Library Management System
--              Covers: users, books, authors, borrowings,
--                      audit logs, notifications
-- =============================================================================

-- -----------------------------------------------------------------------------
-- EXTENSIONS
-- -----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "pgcrypto";      -- gen_random_uuid()
CREATE EXTENSION IF NOT EXISTS "pg_trgm";       -- trigram indexes for search


-- =============================================================================
-- ENUMS
-- =============================================================================

CREATE TYPE user_role AS ENUM (
    'ADMIN',
    'LIBRARIAN',
    'AUTHOR',
    'STUDENT'
);

CREATE TYPE book_type AS ENUM (
    'FICTION',
    'NON_FICTION',
    'SCIENCE',
    'HISTORY',
    'BIOGRAPHY',
    'TECHNOLOGY',
    'PHILOSOPHY',
    'ART',
    'CHILDREN',
    'OTHER'
);

CREATE TYPE borrowing_status AS ENUM (
    'ACTIVE',       -- currently borrowed
    'RETURNED',     -- returned on time or late
    'OVERDUE',      -- past due date, not yet returned
    'LOST'          -- marked as lost by librarian
);

CREATE TYPE notification_type AS ENUM (
    'DUE_DATE_REMINDER',
    'OVERDUE_ALERT',
    'BOOK_AVAILABLE',
    'ACCOUNT_ACTIVITY',
    'SYSTEM_ANNOUNCEMENT'
);

CREATE TYPE audit_event_type AS ENUM (
    'USER_LOGIN',
    'USER_LOGOUT',
    'USER_REGISTER',
    'USER_UPDATE',
    'USER_DELETE',
    'BOOK_CREATE',
    'BOOK_UPDATE',
    'BOOK_DELETE',
    'BORROW_CREATE',
    'BORROW_RETURN',
    'BORROW_EXTEND',
    'ADMIN_ACTION'
);


-- =============================================================================
-- TABLES
-- =============================================================================


-- -----------------------------------------------------------------------------
-- authors
-- -----------------------------------------------------------------------------
CREATE TABLE authors (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    biography       TEXT,
    birth_year      SMALLINT,
    nationality     VARCHAR(100),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE authors IS 'Book authors. A book can have multiple authors (many-to-many via book_authors).';


-- -----------------------------------------------------------------------------
-- books
-- -----------------------------------------------------------------------------
CREATE TABLE books (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    isbn                VARCHAR(20)     UNIQUE,
    title               VARCHAR(500)    NOT NULL,
    description         TEXT,
    book_type           book_type       NOT NULL DEFAULT 'OTHER',
    published_year      SMALLINT,
    publisher           VARCHAR(200),
    cover_image_url     TEXT,
    total_copies        SMALLINT        NOT NULL DEFAULT 1 CHECK (total_copies >= 0),
    available_copies    SMALLINT        NOT NULL DEFAULT 1 CHECK (available_copies >= 0),
    language            VARCHAR(50)     NOT NULL DEFAULT 'English',
    page_count          SMALLINT,
    average_rating      NUMERIC(3, 2)   CHECK (average_rating BETWEEN 0.00 AND 5.00),
    borrow_count        INTEGER         NOT NULL DEFAULT 0,  -- for analytics / trending
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_available_lte_total
        CHECK (available_copies <= total_copies)
);

COMMENT ON TABLE books IS 'Main catalog of library books.';
COMMENT ON COLUMN books.available_copies IS 'Decremented on borrow, incremented on return.';
COMMENT ON COLUMN books.borrow_count IS 'Cumulative borrow count, used by analytics/trending.';


-- -----------------------------------------------------------------------------
-- book_authors  (join table: books <-> authors, many-to-many)
-- -----------------------------------------------------------------------------
CREATE TABLE book_authors (
    book_id     UUID    NOT NULL REFERENCES books(id)   ON DELETE CASCADE,
    author_id   UUID    NOT NULL REFERENCES authors(id) ON DELETE CASCADE,
    PRIMARY KEY (book_id, author_id)
);


-- -----------------------------------------------------------------------------
-- users
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    email                   VARCHAR(255)    NOT NULL UNIQUE,
    username                VARCHAR(100)    NOT NULL UNIQUE,
    password_hash           TEXT            NOT NULL,
    first_name              VARCHAR(100)    NOT NULL,
    last_name               VARCHAR(100)    NOT NULL,
    role                    user_role       NOT NULL DEFAULT 'MEMBER',
    phone_number            VARCHAR(30),
    profile_picture_url     TEXT,
    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    is_email_verified       BOOLEAN         NOT NULL DEFAULT FALSE,
    failed_login_attempts   SMALLINT        NOT NULL DEFAULT 0,
    locked_until            TIMESTAMPTZ,
    last_login_at           TIMESTAMPTZ,
    preferred_genres        book_type[],    -- array of favourite genres (for AI recs)
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE users IS 'Application users: members, librarians, admins.';
COMMENT ON COLUMN users.preferred_genres IS 'User-selected favourite genres used by the recommendation engine.';
COMMENT ON COLUMN users.locked_until IS 'Account temporarily locked after repeated failed logins.';


-- -----------------------------------------------------------------------------
-- refresh_tokens  (for JWT refresh token rotation)
-- -----------------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  TEXT        NOT NULL UNIQUE,    -- store hash, never plaintext
    expires_at  TIMESTAMPTZ NOT NULL,
    revoked     BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens with rotation support.';


-- -----------------------------------------------------------------------------
-- borrowings
-- -----------------------------------------------------------------------------
CREATE TABLE borrowings (
    id              UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID                NOT NULL REFERENCES users(id)  ON DELETE RESTRICT,
    book_id         UUID                NOT NULL REFERENCES books(id)  ON DELETE RESTRICT,
    status          borrowing_status    NOT NULL DEFAULT 'ACTIVE',
    borrowed_at     TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    due_date        TIMESTAMPTZ         NOT NULL,
    returned_at     TIMESTAMPTZ,
    extended_count  SMALLINT            NOT NULL DEFAULT 0,    -- max extensions (e.g. 2)
    fine_amount     NUMERIC(8, 2)       NOT NULL DEFAULT 0.00, -- overdue fine in RON
    fine_paid       BOOLEAN             NOT NULL DEFAULT FALSE,
    notes           TEXT,               -- librarian notes (e.g. "book damaged")
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_due_after_borrowed
        CHECK (due_date > borrowed_at),
    CONSTRAINT chk_returned_after_borrowed
        CHECK (returned_at IS NULL OR returned_at >= borrowed_at)
);

COMMENT ON TABLE borrowings IS 'Each row = one borrow event for one user/book.';
COMMENT ON COLUMN borrowings.extended_count IS 'Number of times the due date was extended.';


-- -----------------------------------------------------------------------------
-- notifications
-- -----------------------------------------------------------------------------
CREATE TABLE notifications (
    id              UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID                NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type            notification_type   NOT NULL,
    title           VARCHAR(255)        NOT NULL,
    message         TEXT                NOT NULL,
    is_read         BOOLEAN             NOT NULL DEFAULT FALSE,
    metadata        JSONB,              -- e.g. {"book_id": "...", "due_date": "..."}
    sent_at         TIMESTAMPTZ         NOT NULL DEFAULT NOW(),
    read_at         TIMESTAMPTZ
);

COMMENT ON TABLE notifications IS 'In-app notifications. Also used as audit trail for emails sent.';
COMMENT ON COLUMN notifications.metadata IS 'Flexible JSONB payload for extra context (book id, link, etc.).';


-- -----------------------------------------------------------------------------
-- audit_logs
-- -----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id              UUID                PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID                REFERENCES users(id) ON DELETE SET NULL, -- nullable: system events
    event_type      audit_event_type    NOT NULL,
    entity_type     VARCHAR(50),        -- e.g. 'Book', 'User', 'Borrowing'
    entity_id       UUID,               -- ID of the affected entity
    description     TEXT,               -- human-readable summary
    old_value       JSONB,              -- snapshot before change
    new_value       JSONB,              -- snapshot after change
    ip_address      INET,
    user_agent      TEXT,
    created_at      TIMESTAMPTZ         NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE audit_logs IS 'Immutable audit trail. Rows are never updated or deleted.';
COMMENT ON COLUMN audit_logs.user_id IS 'NULL for scheduled/system-triggered events.';


-- =============================================================================
-- updated_at TRIGGER  (auto-maintain updated_at on all relevant tables)
-- =============================================================================

CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply to all tables that have updated_at
DO $$
DECLARE
    t TEXT;
BEGIN
    FOREACH t IN ARRAY ARRAY['authors','books','users','borrowings']
    LOOP
        EXECUTE format(
            'CREATE TRIGGER set_updated_at
             BEFORE UPDATE ON %I
             FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at()',
            t
        );
    END LOOP;
END
$$;


-- =============================================================================
-- INDEXES  (performance — mirrors indexes.sql but defined here for completeness;
--           indexes.sql will extend with analytics-specific ones)
-- =============================================================================

-- users
CREATE INDEX idx_users_email           ON users(email);
CREATE INDEX idx_users_username        ON users(username);
CREATE INDEX idx_users_role            ON users(role);
CREATE INDEX idx_users_is_active       ON users(is_active);

-- books  — general lookups
CREATE INDEX idx_books_title           ON books(title);
CREATE INDEX idx_books_isbn            ON books(isbn);
CREATE INDEX idx_books_book_type       ON books(book_type);
CREATE INDEX idx_books_is_active       ON books(is_active);
CREATE INDEX idx_books_available       ON books(available_copies) WHERE available_copies > 0;
CREATE INDEX idx_books_borrow_count    ON books(borrow_count DESC);  -- trending

-- books  — full-text search (trigram)
CREATE INDEX idx_books_title_trgm      ON books USING gin (title gin_trgm_ops);
CREATE INDEX idx_books_desc_trgm       ON books USING gin (description gin_trgm_ops);

-- borrowings
CREATE INDEX idx_borrowings_user_id    ON borrowings(user_id);
CREATE INDEX idx_borrowings_book_id    ON borrowings(book_id);
CREATE INDEX idx_borrowings_status     ON borrowings(status);
CREATE INDEX idx_borrowings_due_date   ON borrowings(due_date);
CREATE INDEX idx_borrowings_active     ON borrowings(user_id, book_id) WHERE status = 'ACTIVE';

-- notifications
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_unread  ON notifications(user_id, is_read) WHERE is_read = FALSE;

-- audit_logs
CREATE INDEX idx_audit_user_id         ON audit_logs(user_id);
CREATE INDEX idx_audit_event_type      ON audit_logs(event_type);
CREATE INDEX idx_audit_entity          ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_created_at      ON audit_logs(created_at DESC);

-- refresh_tokens
CREATE INDEX idx_refresh_tokens_user   ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_exp    ON refresh_tokens(expires_at) WHERE revoked = FALSE;
