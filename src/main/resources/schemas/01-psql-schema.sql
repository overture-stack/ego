CREATE TABLE EGOAPPLICATION (
  id              BIGSERIAL PRIMARY KEY,
  name            VARCHAR(255),
  clientId        VARCHAR(255) UNIQUE,
  clientSecret    VARCHAR(255),
  redirectUri     TEXT,
  description     TEXT,
  status          VARCHAR(36) CHECK (status IN ('Pending', 'Approved', 'Rejected', 'Disabled'))
);


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


CREATE TABLE EGOGROUP (
  id                  BIGSERIAL PRIMARY KEY,
  name                VARCHAR(255) UNIQUE,
  description         VARCHAR(255),
  status              VARCHAR(64) CHECK (status IN ('Pending', 'Approved', 'Rejected', 'Disabled'))
);


CREATE TABLE GROUPAPPLICATION (
  grpId                BIGSERIAL,
  appId                BIGSERIAL,
  PRIMARY KEY (grpId,appId),
  FOREIGN KEY (grpId) REFERENCES EGOGROUP(id),
  FOREIGN KEY (appId) REFERENCES EGOAPPLICATION(id)
);


CREATE TABLE USERGROUP (
  userId                BIGSERIAL,
  grpId                 BIGSERIAL,
  PRIMARY KEY (grpId,userId),
  FOREIGN KEY (grpId)   REFERENCES EGOGROUP(id),
  FOREIGN KEY (userId)  REFERENCES EGOUSER(id)
);


CREATE TABLE USERAPPLICATION (
  userId                BIGSERIAL,
  appId                 BIGSERIAL,
  PRIMARY KEY (userId,appId),
  FOREIGN KEY (appId)   REFERENCES EGOAPPLICATION(id),
  FOREIGN KEY (userId)  REFERENCES EGOUSER(id)
);


CREATE TYPE ACLMASK AS ENUM ('read', 'write', 'deny');


CREATE TABLE ACLENTITY (
  id                    BIGSERIAL PRIMARY KEY,
  owner                 BIGSERIAL,
  name                  varchar(255) UNIQUE,
  FOREIGN KEY (owner)   REFERENCES EGOGROUP(id)
);


CREATE TABLE ACLUSERPERMISSION (
  id                      BIGSERIAL PRIMARY KEY,
  entity                  BIGSERIAL,
  sid                     BIGSERIAL,
  mask                    ACLMASK,
  FOREIGN KEY (entity)    REFERENCES ACLENTITY(id),
  FOREIGN KEY (sid)       REFERENCES EGOUSER(id)
);

CREATE TABLE ACLGROUPPERMISSION (
  id                      BIGSERIAL PRIMARY KEY,
  entity                  BIGSERIAL,
  sid                     BIGSERIAL,
  mask                    ACLMASK,
  FOREIGN KEY (entity)    REFERENCES ACLENTITY(id),
  FOREIGN KEY (sid)       REFERENCES EGOGROUP(id)
);
