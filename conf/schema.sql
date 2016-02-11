CREATE TABLE user (
  username VARCHAR NOT NULL PRIMARY KEY,
  email VARCHAR NOT NULL,
  password_hash VARCHAR,
  salt VARCHAR,
  member_since TIMESTAMP WITH TIME ZONE NOT NULL,
  active BOOLEAN NOT NULL DEFAULT TRUE
);

-- users own (and can share) documents
CREATE TABLE document (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  owner VARCHAR NOT NULL REFERENCES user(username),
  author VARCHAR,
  title VARCHAR NOT NULL,
  date_numeric TIMESTAMP,
  date_freeform VARCHAR,
  description VARCHAR,
  source VARCHAR,
  language VARCHAR
);

-- users can organize documents into folders
CREATE TABLE folder (
   id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
   owner VARCHAR NOT NULL REFERENCES user(username),
   title VARCHAR NOT NULL,
   -- if parent is empty then it's a root folder
   parent INTEGER REFERENCES folder(id)
);

CREATE TABLE folder_association (
	 folder_id INTEGER NOT NULL REFERENCES folder(id),
	 document_id INTEGER NOT NULL REFERENCES document(id)
);

-- teams are a first level entities similar to user
CREATE TABLE team (
  title VARCHAR NOT NULL PRIMARY KEY,
  created_by VARCHAR NOT NULL REFERENCES user(username),
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE team_membership (
  username VARCHAR NOT NULL REFERENCES user(username),
  team VARCHAR NOT NULL REFERENCES team(title),
  member_since TIMESTAMP WITH TIME ZONE NOT NULL
);

-- ledger of shared documents and folders
CREATE TABLE sharing_record (
  id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
  -- one of the following two needs to be defined
  folder_id INTEGER REFERENCES folder(id),
  document_id INTEGER REFERENCES document(id),
  shared_by VARCHAR NOT NULL REFERENCES user(username),
  shared_with VARCHAR NOT NULL REFERENCES user(username),
  shared_at TIMESTAMP WITH TIME ZONE NOT NULL,
  accepted BOOLEAN NOT NULL DEFAULT FALSE,
  revoked BOOLEAN NOT NULL DEFAULT FALSE
  -- active boolean NOT NULL DEFAULT 'true',
  -- deactivation_date timestamp with time zone
);

-- restrict available document sharing possibilities
-- create table sharing_policies_codes (
--  code varchar primary key CHECK (code in ('Folder', 'Document'))
-- );
-- insert into sharing_policies_codes values ('Folder'), ('Document');

-- create table sharing_event_codes (
--   code varchar primary key CHECK (code in ('added', 'deleted','moved','accepted','declined'))
-- );
-- insert into sharing_event_codes values ('added'), ('deleted'), ('moved'), ('accepted'), ('declined');

-- keep a log what happened for shared elements e.g. removal of documents
-- to inform the user about what happened
-- create table sharing_event_log (
-- id integer NOT NULL PRIMARY KEY AUTOINCREMENT,
--  userid varchar NOT NULL REFERENCES users(username),
--  code varchar references sharing_event_codes (code),
--  sharingid integer references sharing_policies(id),
--  folderid integer references folders(id),
--  documentid integer references documents(id),
--  event_date timestamp with time zone NOT NULL
-- );

-- tags are user specific and allow him/her to group documents
-- CREATE TABLE hashtags (
--   id integer NOT NULL PRIMARY KEY AUTOINCREMENT,
--   userid varchar NOT NULL REFERENCES users(username),
--   name varchar NOT NULL
-- );

-- CREATE TABLE hashtags_documents(
--    tagid integer NOT NULL REFERENCES tags(id),
--    docid integer NOT NULL REFERENCES documents(id)
-- );
