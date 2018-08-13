CREATE TYPE ACLMASK AS ENUM ('READ', 'WRITE', 'DENY');

CREATE TABLE ACLENTITY (
  id                    UUID PRIMARY KEY,
  owner                 UUID,
  name                  varchar(255) UNIQUE NOT NULL,
  FOREIGN KEY (owner)   REFERENCES EGOGROUP(id)
);


CREATE TABLE ACLUSERPERMISSION (
  id                      UUID PRIMARY KEY,
  entity                  UUID,
  sid                     UUID,
  mask                    ACLMASK NOT NULL,
  FOREIGN KEY (entity)    REFERENCES ACLENTITY(id),
  FOREIGN KEY (sid)       REFERENCES EGOUSER(id)
);


CREATE TABLE ACLGROUPPERMISSION (
  id                      UUID PRIMARY KEY,
  entity                  UUID,
  sid                     UUID,
  mask                    ACLMASK NOT NULL,
  FOREIGN KEY (entity)    REFERENCES ACLENTITY(id),
  FOREIGN KEY (sid)       REFERENCES EGOGROUP(id)
);