CREATE TABLE task_lists (
    id UUID PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_task_lists_name UNIQUE (name)
);

ALTER TABLE tasks ADD COLUMN task_list_id UUID;
ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_task_list
    FOREIGN KEY (task_list_id) REFERENCES task_lists (id);
CREATE INDEX idx_tasks_task_list_id ON tasks (task_list_id);
