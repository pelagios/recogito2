CREATE TABLE users (
  username varchar primary key,
  email varchar NOT NULL,
  password_hash varchar,
  salt varchar,
  member_since timestamp with time zone NOT NULL
);

CREATE TABLE documents (
  id integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  ownerid varchar NOT NULL REFERENCES users(username),
  title varchar NOT NULL,
  author varchar,
  description varchar,
  language varchar
);

--groups are a first level entities similar to user
CREATE TABLE groups (
  id integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  ownerid varchar NOT NULL REFERENCES users(username),
  title varchar NOT NULL,
  creation_since timestamp with time zone NOT NULL
);

CREATE TABLE groups_members(
  userid varchar NOT NULL REFERENCES users(username),
  groupid varchar NOT NULL REFERENCES groups(id),
  member_since timestamp with time zone NOT NULL
);

--tags are user specific and allow him/her to group documents
CREATE TABLE hashtags(
  id integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  userid varchar NOT NULL REFERENCES users(username),
  name varchar NOT NULL
);

CREATE TABLE hashtags_documents(
   tagid integer NOT NULL REFERENCES tags(id),
   docid integer NOT NULL REFERENCES documents(id)
);

-- restrict available document sharing possibilities
create table sharing_policies_codes (
  code varchar primary key CHECK (code in ('Folder', 'Document'))
);
insert into sharing_policies_codes values ('Folder'), ('Document');

-- document and folder sharing between users
create table sharing_policies ( 
  id integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  code varchar references sharing_policies_codes (code),
  folderId integer references folders(id),
  documentId integer references documents(id),
  fromuserid varchar NOT NULL REFERENCES users(username),
  withuserid varchar NOT NULL REFERENCES users(username),
  active boolean NOT NULL DEFAULT 'true',
  creation_date timestamp with time zone NOT NULL,
  deactivation_date timestamp with time zone
);

create table sharing_event_codes (
  code varchar primary key CHECK (code in ('added', 'deleted','moved','accepted','declined'))
);
insert into sharing_event_codes values ('added'), ('deleted'), ('moved'), ('accepted'), ('declined');

--keep a log what happened for shared elements e.g. removal of documents 
--to inform the user about what happened
create table sharing_event_log (
 id integer NOT NULL PRIMARY KEY AUTOINCREMENT,
 userid varchar NOT NULL REFERENCES users(username),
 code varchar references sharing_event_codes (code),
 sharingid integer references sharing_policies(id),
 folderid integer references folders(id),
 documentid integer references documents(id),
 event_date timestamp with time zone NOT NULL
);

-- the folder structure is managed by user so the same shared document can be located in different folder for different users
create table folders(
   id integer NOT NULL PRIMARY KEY AUTOINCREMENT,
   userid varchar NOT NULL REFERENCES users(username),
   title varchar NOT NULL,
   --if parent is empty then it's the root folder
   parent integer REFERENCES folders(id)
);

create table folders_documents(
	 folderid integer NOT NULL REFERENCES folders(id),
	 documentid integer NOT NULL REFERENCES documents(id)
);
