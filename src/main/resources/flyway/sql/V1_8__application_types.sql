CREATE TYPE EGOTYPE AS ENUM('USER','ADMIN');
ALTER TABLE EGOUSER RENAME COLUMN role to type;
ALTER TABLE EGOAPPLICATION add column type EGOTYPE not null;