DROP TABLE IF EXISTS EGOAPPLICATION CASCADE;
CREATE TABLE EGOAPPLICATION (
  appId           BIGSERIAL PRIMARY KEY,
  appName         VARCHAR(255) UNIQUE,
  clientId        VARCHAR(255),
  clientSecret    VARCHAR(255),
  redirectUri     TEXT,
  description     TEXT,
  status          VARCHAR(36) CHECK (status IN ('Pending', 'Approved', 'Rejected', 'Disabled'))
);

DROP TABLE IF EXISTS EGOUSER CASCADE;
CREATE TABLE EGOUSER (
  userid            BIGSERIAL PRIMARY KEY,
  userName          VARCHAR(255) UNIQUE,
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
  grpId              BIGSERIAL PRIMARY KEY,
  grpName            VARCHAR(255) UNIQUE,
  description        VARCHAR(255),
  status             VARCHAR(64) CHECK (status IN ('Pending', 'Approved', 'Rejected', 'Disabled'))
);

DROP TABLE IF EXISTS GROUPAPPLICATION CASCADE;
CREATE TABLE GROUPAPPLICATION (
  grpName                VARCHAR(255),
  appName                VARCHAR(255),
  PRIMARY KEY (grpName,appName),
  FOREIGN KEY (grpName) REFERENCES EGOGROUP(grpName),
  FOREIGN KEY (appName) REFERENCES EGOAPPLICATION(appName)
);

DROP TABLE IF EXISTS USERGROUP CASCADE;
CREATE TABLE USERGROUP (
  userName                VARCHAR(255),
  grpName                 VARCHAR(255),
  PRIMARY KEY (grpName,userName),
  FOREIGN KEY (grpName) REFERENCES EGOGROUP(grpName),
  FOREIGN KEY (userName) REFERENCES EGOUSER(userName)
);

DROP TABLE IF EXISTS USERAPPLICATION CASCADE;
CREATE TABLE USERAPPLICATION (
  userName                VARCHAR(255),
  appName                 VARCHAR(255),
  PRIMARY KEY (userName,appName),
  FOREIGN KEY (appName) REFERENCES EGOAPPLICATION(appName),
  FOREIGN KEY (userName) REFERENCES EGOUSER(userName)
);


