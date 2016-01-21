CREATE TABLE users (
  username varchar primary key,
  email varchar NOT NULL,
  member_since timestamp with time zone NOT NULL
);
