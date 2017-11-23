DROP TABLE IF EXISTS EGOAPPLICATION CASCADE;
CREATE TABLE EGOAPPLICATION (
  id              BIGSERIAL PRIMARY KEY,
  name            VARCHAR(255) UNIQUE,
  clientId        VARCHAR(255),
  clientSecret    VARCHAR(255),
  redirectUri     TEXT,
  description     TEXT,
  status          VARCHAR(36) CHECK (status IN ('Pending', 'Approved', 'Rejected', 'Disabled'))
);

DROP TABLE IF EXISTS EGOUSER CASCADE;
CREATE TABLE EGOUSER (
  id                BIGSERIAL PRIMARY KEY,
  name              VARCHAR(255) UNIQUE,
  email             VARCHAR(255) UNIQUE,
  role              VARCHAR(64),
  firstName         TEXT,
  lastName          TEXT,
  createdAt         VARCHAR(64),
  lastLogin         VARCHAR(64),
  status            VARCHAR(36) CHECK (status IN ('Pending', 'Approved', 'Rejected', 'Disabled')),
  preferredLanguage VARCHAR(36) CHECK (preferredLanguage IN ('English', 'French', 'Spanish'))
);

DROP TABLE IF EXISTS EGOGROUP CASCADE;
CREATE TABLE EGOGROUP (
  id                  BIGSERIAL PRIMARY KEY,
  name                VARCHAR(255) UNIQUE,
  description         VARCHAR(255),
  status              VARCHAR(64) CHECK (status IN ('Pending', 'Approved', 'Rejected', 'Disabled'))
);

DROP TABLE IF EXISTS GROUPAPPLICATION CASCADE;
CREATE TABLE GROUPAPPLICATION (
  grpId                BIGSERIAL,
  appId                BIGSERIAL,
  PRIMARY KEY (grpId,appId),
  FOREIGN KEY (grpId) REFERENCES EGOGROUP(id),
  FOREIGN KEY (appId) REFERENCES EGOAPPLICATION(id)
);

DROP TABLE IF EXISTS USERGROUP CASCADE;
CREATE TABLE USERGROUP (
  userId                BIGSERIAL,
  grpId                 BIGSERIAL,
  PRIMARY KEY (grpId,userId),
  FOREIGN KEY (grpId)   REFERENCES EGOGROUP(id),
  FOREIGN KEY (userId)  REFERENCES EGOUSER(id)
);

DROP TABLE IF EXISTS USERAPPLICATION CASCADE;
CREATE TABLE USERAPPLICATION (
  userId                BIGSERIAL,
  appId                 BIGSERIAL,
  PRIMARY KEY (userId,appId),
  FOREIGN KEY (appId)   REFERENCES EGOAPPLICATION(id),
  FOREIGN KEY (userId)  REFERENCES EGOUSER(id)
);


