ALTER TABLE egouser ALTER COLUMN email DROP NOT NULL;
ALTER TABLE egouser DROP CONSTRAINT egouser_email_key;

ALTER TABLE egouser ALTER COLUMN name DROP NOT NULL;
ALTER TABLE egouser DROP CONSTRAINT egouser_name_key;

CREATE TYPE providername AS ENUM('GOOGLE', 'FACEBOOK', 'LINKEDIN', 'GITHUB', 'ORCID', 'EMPTY');
ALTER TABLE egouser ADD COLUMN identityprovider providername NOT NULL DEFAULT 'EMPTY';

ALTER TABLE egouser ADD COLUMN providerid VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE egouser ADD UNIQUE(identityprovider, providerid);
