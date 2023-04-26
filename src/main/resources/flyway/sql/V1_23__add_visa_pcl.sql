CREATE TABLE ACLVISAPERMISSION (
  id                      UUID PRIMARY KEY,
  entity                  UUID,
  visaId                  UUID,
  mask                    ACLMASK NOT NULL,
  FOREIGN KEY (entity)    REFERENCES POLICY(id),
  FOREIGN KEY (visaId)    REFERENCES GA4GHVISA(id)
);
