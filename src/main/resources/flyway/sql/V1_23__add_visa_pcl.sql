CREATE TABLE ACLVISAPERMISSION (
  id                      UUID PRIMARY KEY,
  policy_id               UUID,
  visa_id                  UUID,
  access_level            ACLMASK NOT NULL,
  FOREIGN KEY (policy_id) REFERENCES POLICY(id),
  FOREIGN KEY (visa_id)    REFERENCES GA4GHVISA(id)
);
