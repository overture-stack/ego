ALTER TABLE token RENAME COLUMN token TO name;
ALTER TABLE token ADD CONSTRAINT token_name_key UNIQUE (name);
ALTER TABLE token ADD description VARCHAR(255);
