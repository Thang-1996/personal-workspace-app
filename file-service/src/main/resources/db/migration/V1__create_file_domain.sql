CREATE TABLE folders (
  id UUID PRIMARY KEY, owner_id UUID NOT NULL, parent_id UUID,
  name VARCHAR(120) NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT fk_folders_parent FOREIGN KEY (parent_id) REFERENCES folders(id)
);
ALTER TABLE folders
  ADD CONSTRAINT uk_folders_owner_parent_name UNIQUE(owner_id, parent_id, name);
CREATE INDEX idx_folders_owner_parent ON folders(owner_id, parent_id);

CREATE TABLE files (
  id UUID PRIMARY KEY, owner_id UUID NOT NULL, original_name VARCHAR(255) NOT NULL,
  storage_key VARCHAR(255) NOT NULL UNIQUE, content_type VARCHAR(150) NOT NULL,
  size_bytes BIGINT NOT NULL, checksum VARCHAR(64) NOT NULL, folder_id UUID,
  status VARCHAR(20) NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT fk_files_folder FOREIGN KEY (folder_id) REFERENCES folders(id)
);
CREATE INDEX idx_files_owner_folder_created ON files(owner_id, folder_id, created_at DESC);

CREATE TABLE file_links (
  id UUID PRIMARY KEY, file_id UUID NOT NULL, linked_entity_type VARCHAR(40) NOT NULL,
  linked_entity_id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  CONSTRAINT fk_file_links_file FOREIGN KEY (file_id) REFERENCES files(id),
  CONSTRAINT uk_file_link UNIQUE(file_id, linked_entity_type, linked_entity_id)
);
