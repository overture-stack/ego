DROP TABLE IF EXISTS APPLICATION CASCADE;
CREATE TABLE APPLICATION (id VARCHAR(64) PRIMARY KEY, applicationName VARCHAR(255), clientId VARCHAR(255),
  clientSecret VARCHAR(255), redirectUri TEXT, description TEXT,
  status VARCHAR(36) check (status in ('Pending', 'Approved', 'Rejected', 'Disabled')));

ALTER TABLE APPLICATION ALTER COLUMN ID VARCHAR(64) NOT NULL;

DROP TABLE IF EXISTS USERS CASCADE;
CREATE TABLE USERS (id VARCHAR(255) PRIMARY KEY, userName VARCHAR(255), email VARCHAR(255),
  role VARCHAR(64), firstName TEXT, lastName TEXT, createdAt VARCHAR(64), lastLogin VARCHAR(64),
  status VARCHAR(36),
  preferredLanguage VARCHAR(36) check (preferredLanguage in ('English', 'French', 'Spanish')));

