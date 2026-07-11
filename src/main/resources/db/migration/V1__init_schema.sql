-- V1__init_schema.sql
-- Initial PostgreSQL schema for the Personal Diary app, migrated from MongoDB.
--
-- Design decisions (see conversation with Claude for full reasoning):
--   1. IDs are BIGSERIAL (auto-incrementing bigints), replacing Mongo's ObjectId.
--   2. diary_entries owns the foreign key to users (user_id) instead of the old
--      Mongo pattern of users holding an array of @DBRef entry IDs. This makes
--      the relationship enforced by the database itself instead of app code.
--   3. Roles are modeled as a single hierarchical column (USER or ADMIN), not a
--      separate multi-valued roles table, because ADMIN is a strict superset of
--      USER privileges in this app (enforced via Spring Security's RoleHierarchy),
--      not an independent, combinable grant.
--   4. created_at/updated_at use TIMESTAMPTZ (not TIMESTAMP) so stored moments
--      are unambiguous regardless of app/db server timezone. updated_at's
--      "refresh on every edit" behavior is handled by Hibernate's
--      @UpdateTimestamp in the entity classes, not by the database itself.
--   5. On diary_entries: content is now the required field and title is
--      optional (reversed from the original design) -- the diary text itself
--      is the actual entry; a title is just an optional label. content is
--      TEXT with a CHECK constraint instead of VARCHAR(n), since Postgres
--      gets no storage/performance benefit from a VARCHAR length cap -- the
--      CHECK expresses the same 20,000-character limit explicitly.
--      NOTE: corresponding Java-side validation (DiaryEntry.java: move
--      @NotBlank from title to content, adjust @Size) still needs updating
--      to match -- pending as of this migration.

CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    user_name VARCHAR(30) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER' CHECK (role IN ('USER', 'ADMIN')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE diary_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(200),
    content TEXT NOT NULL CHECK (char_length(content) <= 20000),
    entry_date TIMESTAMP NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_diary_entries_user_id ON diary_entries(user_id);

CREATE TABLE config_diary_app (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(255) NOT NULL UNIQUE,
    config_value VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);