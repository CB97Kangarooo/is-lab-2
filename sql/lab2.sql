ALTER TABLE app_user ADD COLUMN IF NOT EXISTS role VARCHAR(5) NOT NULL DEFAULT 'USER'
    CHECK (role IN ('USER', 'ADMIN'));

CREATE TABLE IF NOT EXISTS import_operation (
    id BIGSERIAL PRIMARY KEY,
    initiator_id BIGINT NOT NULL REFERENCES app_user(id),
    status VARCHAR(11) NOT NULL CHECK (status IN ('IN_PROGRESS', 'SUCCESS', 'FAILED')),
    added_count INTEGER,
    CHECK ((status = 'SUCCESS' AND added_count IS NOT NULL AND added_count > 0)
        OR (status <> 'SUCCESS' AND added_count IS NULL))
);

CREATE INDEX IF NOT EXISTS ix_import_operation_initiator
    ON import_operation (initiator_id, id DESC);
