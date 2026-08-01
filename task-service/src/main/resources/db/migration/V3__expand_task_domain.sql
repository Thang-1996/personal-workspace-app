ALTER TABLE task_lists ADD COLUMN owner_id UUID;
ALTER TABLE task_lists ADD COLUMN color VARCHAR(20);
ALTER TABLE task_lists ADD COLUMN position INTEGER NOT NULL DEFAULT 0;
ALTER TABLE task_lists ADD COLUMN archived BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE task_lists SET owner_id = '00000000-0000-0000-0000-000000000001' WHERE owner_id IS NULL;
ALTER TABLE task_lists ALTER COLUMN owner_id SET NOT NULL;
ALTER TABLE task_lists DROP CONSTRAINT uk_task_lists_name;
ALTER TABLE task_lists ADD CONSTRAINT uk_task_lists_owner_name UNIQUE (owner_id, name);

ALTER TABLE tasks ADD COLUMN owner_id UUID;
ALTER TABLE tasks ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';
ALTER TABLE tasks ADD COLUMN due_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tasks ADD COLUMN completed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE tasks ADD COLUMN position INTEGER NOT NULL DEFAULT 0;
UPDATE tasks SET owner_id = '00000000-0000-0000-0000-000000000001' WHERE owner_id IS NULL;
ALTER TABLE tasks ALTER COLUMN owner_id SET NOT NULL;

CREATE TABLE task_tags (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    color VARCHAR(20),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_task_tags_owner_name UNIQUE (owner_id, name)
);

CREATE TABLE task_tag_relations (
    task_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    PRIMARY KEY (task_id, tag_id),
    CONSTRAINT fk_task_tag_relations_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_tag_relations_tag FOREIGN KEY (tag_id) REFERENCES task_tags (id) ON DELETE CASCADE
);

CREATE TABLE task_comments (
    id UUID PRIMARY KEY,
    task_id UUID NOT NULL,
    author_id UUID NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_task_comments_task FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE
);

CREATE INDEX idx_tasks_owner_status ON tasks (owner_id, status);
CREATE INDEX idx_tasks_owner_due_at ON tasks (owner_id, due_at);
CREATE INDEX idx_tasks_list_position ON tasks (task_list_id, position);
CREATE INDEX idx_task_lists_owner_archived ON task_lists (owner_id, archived);
CREATE INDEX idx_task_comments_task ON task_comments (task_id);
