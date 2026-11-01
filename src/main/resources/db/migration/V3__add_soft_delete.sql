-- V3__add_soft_delete_for_diary_entries.sql
-- Adds soft-delete support for diary entries.

ALTER TABLE diary_entries ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE diary_entries ADD COLUMN deleted_at TIMESTAMPTZ;
CREATE INDEX idx_diary_entries_deleted ON diary_entries(deleted);
