ALTER TABLE notifications DROP CONSTRAINT IF EXISTS fk_notifications_document;
ALTER TABLE notifications DROP COLUMN IF EXISTS document_id;

DROP TABLE IF EXISTS document_chunks;
DROP TABLE IF EXISTS documents;
