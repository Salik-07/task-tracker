-- V1: initial schema for task lists and their tasks.
-- Flyway runs this once and records it in flyway_schema_history.
-- Never edit this file after it has been applied: add a V2__... instead.

CREATE TABLE task_list (
    -- uuidv7() is built into Postgres 18: time-ordered UUIDs, so new rows land
    -- at the end of the index instead of at random places (like random v4 UUIDs).
    id          uuid         PRIMARY KEY DEFAULT uuidv7(),
    title       varchar(255) NOT NULL CHECK (btrim(title) <> ''),
    description text,
    -- timestamptz stores an absolute instant (maps to java.time.Instant).
    created_at  timestamptz  NOT NULL DEFAULT now(),
    updated_at  timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE task (
    id           uuid         PRIMARY KEY DEFAULT uuidv7(),
    -- Deleting a list deletes its tasks in the database itself.
    task_list_id uuid         NOT NULL REFERENCES task_list (id) ON DELETE CASCADE,
    title        varchar(255) NOT NULL CHECK (btrim(title) <> ''),
    description  text,
    due_date     date,
    -- Enums are stored as text. The CHECK is the database's own guard,
    -- even against rows inserted outside the app.
    status       varchar(20)  NOT NULL DEFAULT 'OPEN'
                 CHECK (status IN ('OPEN', 'COMPLETED')),
    priority     varchar(20)  NOT NULL DEFAULT 'MEDIUM'
                 CHECK (priority IN ('HIGH', 'MEDIUM', 'LOW')),
    created_at   timestamptz  NOT NULL DEFAULT now(),
    updated_at   timestamptz  NOT NULL DEFAULT now()
);

-- Postgres indexes primary keys automatically, but not foreign keys.
-- Without this, "all tasks of list X" would scan the whole task table.
CREATE INDEX idx_task_task_list_id ON task (task_list_id);
