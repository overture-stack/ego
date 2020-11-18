ALTER TABLE egouser ALTER COLUMN email DROP NOT NULL;
ALTER TABLE egouser DROP CONSTRAINT egouser_email_key;

ALTER TABLE egouser ALTER COLUMN name DROP NOT NULL;
ALTER TABLE egouser DROP CONSTRAINT egouser_name_key;

CREATE TYPE providerType AS ENUM('GOOGLE', 'FACEBOOK', 'LINKEDIN', 'GITHUB', 'ORCID');
ALTER TABLE egouser ADD COLUMN providertype providerType;
ALTER TABLE egouser ALTER COLUMN providertype SET NOT NULL;

ALTER TABLE egouser ADD COLUMN providerid VARCHAR(255);
ALTER TABLE egouser ALTER COLUMN providerid SET NOT NULL;

ALTER TABLE egouser ADD UNIQUE(providertype, providerid);
