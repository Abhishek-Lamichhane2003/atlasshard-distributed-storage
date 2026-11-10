CREATE TABLE users (
  id varchar(36) PRIMARY KEY,
  email varchar(255) NOT NULL UNIQUE,
  password_hash varchar(255) NOT NULL,
  created_at timestamptz NOT NULL
);

CREATE TABLE files (
  id varchar(36) PRIMARY KEY,
  owner_id varchar(36) NOT NULL,
  file_name varchar(255) NOT NULL,
  logical_path varchar(1024) NOT NULL,
  content_hash varchar(64) NOT NULL,
  object_key varchar(255) NOT NULL,
  size_bytes bigint NOT NULL,
  version_number integer NOT NULL,
  created_at timestamptz NOT NULL
);
CREATE INDEX idx_files_owner ON files(owner_id);
CREATE INDEX idx_files_hash ON files(content_hash);
CREATE INDEX idx_files_path ON files(owner_id, logical_path, version_number DESC);
CREATE UNIQUE INDEX uq_files_owner_path_version ON files(owner_id, logical_path, version_number);

CREATE TABLE share_links (
  id varchar(36) PRIMARY KEY,
  file_id varchar(36) NOT NULL,
  owner_id varchar(36) NOT NULL,
  token_hash varchar(64) NOT NULL UNIQUE,
  expires_at timestamptz NOT NULL,
  created_at timestamptz NOT NULL
);
CREATE INDEX idx_share_owner ON share_links(owner_id);
CREATE INDEX idx_share_expiry ON share_links(expires_at);

CREATE TABLE audit_events (
  id varchar(36) PRIMARY KEY,
  type varchar(255) NOT NULL,
  detail varchar(1000) NOT NULL,
  created_at timestamptz NOT NULL
);
