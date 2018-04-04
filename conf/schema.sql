-- note: 'user' is a keyword in posgres, so we need those quotes
CREATE TABLE "user" (
  username TEXT NOT NULL PRIMARY KEY,
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  salt TEXT NOT NULL,
  member_since TIMESTAMP WITH TIME ZONE NOT NULL,
  real_name TEXT,
  bio TEXT,
  website TEXT,
  quota_mb INT NOT NULL DEFAULT 200,
  last_login TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_user_email ON "user"(email);

CREATE TABLE user_role (
  id SERIAL PRIMARY KEY,
  username TEXT NOT NULL REFERENCES "user"(username),
  has_role TEXT NOT NULL
);
CREATE INDEX idx_user_role_username ON user_role(username);

CREATE TABLE feature_toggle (
  id SERIAL PRIMARY KEY,
  username TEXT NOT NULL REFERENCES "user"(username),
  has_toggle TEXT NOT NULL
);
CREATE INDEX idx_feature_toggle_username ON feature_toggle(username);

-- staging area for documents during upload workflow
CREATE TABLE upload (
  id SERIAL PRIMARY KEY,
  owner TEXT NOT NULL UNIQUE REFERENCES "user"(username),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  title TEXT NOT NULL,
  author TEXT,
  date_freeform TEXT,
  description TEXT,
  language TEXT,
  source TEXT,
  edition TEXT,
  license TEXT
);

CREATE TABLE upload_filepart (
  id UUID PRIMARY KEY,
  upload_id INTEGER NOT NULL REFERENCES upload(id) ON DELETE CASCADE,
  owner TEXT NOT NULL REFERENCES "user"(username),
  title TEXT NOT NULL,
  content_type TEXT NOT NULL,
  file TEXT NOT NULL,
  filesize_kb DOUBLE PRECISION,
  source text,
  sequence_no INTEGER,
  UNIQUE (owner, title)
);

-- users own (and can share) documents
CREATE TABLE document (
  id TEXT NOT NULL PRIMARY KEY,
  owner TEXT NOT NULL REFERENCES "user"(username),
  uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
  title TEXT NOT NULL,
  author TEXT,
  date_numeric TIMESTAMP,
  date_freeform TEXT,
  description TEXT,
  language TEXT,
  source TEXT,
  edition TEXT,
  license TEXT,
  attribution TEXT,
  public_visibility TEXT NOT NULL DEFAULT 'PRIVATE',
  public_access_level TEXT
);
CREATE INDEX idx_document_owner ON document(owner);

CREATE TABLE document_filepart (
  id UUID PRIMARY KEY,
  document_id TEXT NOT NULL REFERENCES document(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  content_type TEXT NOT NULL,
  file TEXT NOT NULL,
  sequence_no INTEGER NOT NULL,
  source TEXT
);
CREATE INDEX idx_document_filepart_document_id ON document_filepart(document_id);

-- users can organize documents into folders
CREATE TABLE folder (
  id SERIAL PRIMARY KEY,
  owner TEXT NOT NULL REFERENCES "user"(username),
  title TEXT NOT NULL,
  -- if parent is empty then it's a root folder
  parent INTEGER REFERENCES folder(id)
);

CREATE TABLE folder_association (
  folder_id INTEGER NOT NULL REFERENCES folder(id),
  document_id TEXT NOT NULL REFERENCES document(id)
);

-- ledger of shared documents and folders
CREATE TABLE sharing_policy (
  id SERIAL PRIMARY KEY,
  -- one of the following two needs to be defined
  folder_id INTEGER REFERENCES folder(id),
  document_id TEXT REFERENCES document(id),
  shared_by TEXT NOT NULL REFERENCES "user"(username),
  shared_with TEXT NOT NULL REFERENCES "user"(username),
  shared_at TIMESTAMP WITH TIME ZONE NOT NULL,
  access_level TEXT NOT NULL,
  UNIQUE (document_id, shared_with)
);
CREATE INDEX idx_sharing_policy_document_id ON sharing_policy(document_id);
CREATE INDEX idx_sharing_policy_shared_with ON sharing_policy(shared_with);

CREATE TABLE task (
  id UUID PRIMARY KEY,
  task_type TEXT NOT NULL,
  class_name TEXT NOT NULL,
  -- some tasks run on specific documents and/or fileparts
  document_id TEXT,
  filepart_id UUID,
  spawned_by TEXT,
  spawned_at TIMESTAMP WITH TIME ZONE NOT NULL,
  stopped_at TIMESTAMP WITH TIME ZONE,
  -- all-purpose text field for holding results, exception message, etc.
  stopped_with TEXT,
  status TEXT NOT NULL DEFAULT 'PENDING',
  progress INTEGER NOT NULL
);

CREATE TABLE service_announcement (
  id UUID PRIMARY KEY,
  for_user TEXT NOT NULL REFERENCES "user"(username),
  content TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  viewed_at TIMESTAMP WITH TIME ZONE,
  response TEXT
);
CREATE INDEX idx_service_announcement_for_user ON service_announcement(for_user);
CREATE INDEX idx_service_announcement_created_at ON service_announcement(created_at);
