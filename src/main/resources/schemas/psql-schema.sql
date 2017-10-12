DROP TABLE IF EXISTS APPLICATION CASCADE;
CREATE TABLE APPLICATIONS (
  id              VARCHAR(64) PRIMARY KEY NOT NULL,
  applicationName VARCHAR(255),
  clientId        VARCHAR(255),
  clientSecret    VARCHAR(255),
  redirectUri     TEXT,
  description     TEXT,
  status          VARCHAR(36) CHECK (status IN ('Pending', 'Approved', 'Rejected', 'Disabled'))
);

DROP TABLE IF EXISTS EGOUSER CASCADE;
CREATE TABLE EGOUSER (
  id                BIGSERIAL PRIMARY KEY,
  userName          VARCHAR(255),
  email             VARCHAR(255),
  role              VARCHAR(64),
  firstName         TEXT,
  lastName          TEXT,
  createdAt         VARCHAR(64),
  lastLogin         VARCHAR(64),
  status            VARCHAR(36),
  preferredLanguage VARCHAR(36) CHECK (preferredLanguage IN ('English', 'French', 'Spanish'))
);

