CREATE TABLE users (
  username varchar primary key,
  email varchar NOT NULL,
  password_hash varchar,
  salt varchar,
  member_since timestamp with time zone NOT NULL
);

CREATE TABLE documents (
  id integer NOT NULL PRIMARY KEY AUTOINCREMENT,
  username varchar NOT NULL REFERENCES users(username),
  title varchar NOT NULL,
  author varchar,
  description varchar,
  language varchar
);
