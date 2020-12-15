ALTER TABLE egouser ALTER COLUMN email DROP NOT NULL;
ALTER TABLE egouser DROP CONSTRAINT egouser_email_key;

ALTER TABLE egouser ALTER COLUMN name DROP NOT NULL;
ALTER TABLE egouser DROP CONSTRAINT egouser_name_key;

CREATE TYPE providerType AS ENUM('GOOGLE', 'FACEBOOK', 'LINKEDIN', 'GITHUB', 'ORCID');
ALTER TABLE egouser ADD COLUMN providertype providerType;
ALTER TABLE egouser ALTER COLUMN providertype SET DEFAULT '${default_provider}';
-- default values are not added to existing rows, need to explicitly update where providertype is NULL
UPDATE egouser SET providertype = DEFAULT WHERE providertype IS NULL;
-- then set not null constraint
ALTER TABLE egouser ALTER COLUMN providertype SET NOT NULL;

ALTER TABLE egouser ADD COLUMN providerid VARCHAR(255);
UPDATE egouser SET providerid = email WHERE providerid IS NULL;
ALTER TABLE egouser ALTER COLUMN providerid SET NOT NULL;

ALTER TABLE egouser ADD UNIQUE(providertype, providerid);

-- create tripwire table for verifying configured default provider
CREATE TABLE defaultprovidertripwire (
    id providerType PRIMARY KEY
);

INSERT INTO defaultprovidertripwire (id) VALUES ('${default_provider}');
