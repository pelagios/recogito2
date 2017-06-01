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
  quota_mb INT NOT NULL DEFAULT 200
);

CREATE TABLE user_role (
  id SERIAL PRIMARY KEY,
  username TEXT NOT NULL REFERENCES "user"(username),
  has_role TEXT NOT NULL
);

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
  is_public BOOLEAN NOT NULL DEFAULT FALSE,
  attribution TEXT
);

CREATE TABLE document_filepart (
  id UUID PRIMARY KEY,
  document_id TEXT NOT NULL REFERENCES document(id) ON DELETE CASCADE,
  title TEXT NOT NULL,
  content_type TEXT NOT NULL,
  file TEXT NOT NULL,
  sequence_no INTEGER NOT NULL,
  source TEXT
);

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

-- keep a log of what happened for shared elements
-- e.g. to inform users about what happened
-- CREATE TABLE sharing_event_log (
--   id SERIAL PRIMARY KEY,
--   type_of_action TEXT,
--   action_by TEXT NOT NULL REFERENCES "user"(username),
--   action_at TIMESTAMP WITH TIME ZONE NOT NULL,
--   policy_id INTEGER NOT NULL REFERENCES sharing_policy(id)
-- );

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
