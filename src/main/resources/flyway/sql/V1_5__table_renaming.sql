ALTER TABLE ACLENTITY RENAME TO POLICY;
ALTER TABLE POLICY RENAME CONSTRAINT ACLENTITY_PKEY TO POLICY_PKEY;
ALTER TABLE POLICY RENAME CONSTRAINT ACLENTITY_NAME_KEY TO POLICY_NAME_KEY;

ALTER TABLE ACLUSERPERMISSION RENAME TO USERPERMISSION;
ALTER TABLE USERPERMISSION RENAME ENTITY TO POLICY_ID;
ALTER TABLE USERPERMISSION RENAME SID TO USER_ID;
ALTER TABLE USERPERMISSION RENAME MASK TO ACCESS_LEVEL;
ALTER TABLE USERPERMISSION RENAME CONSTRAINT ACLUSERPERMISSION_PKEY TO USERPERMISSION_PKEY;
ALTER TABLE USERPERMISSION RENAME CONSTRAINT ACLUSERPERMISSION_ENTITY_FKEY TO USERPERMISSION_POLICY_FKEY;
ALTER TABLE USERPERMISSION RENAME CONSTRAINT ACLUSERPERMISSION_SID_FKEY TO USERPERMISSION_USER_FKEY;

ALTER TABLE ACLGROUPPERMISSION RENAME TO GROUPPERMISSION;
ALTER TABLE GROUPPERMISSION RENAME ENTITY TO POLICY_ID;
ALTER TABLE GROUPPERMISSION RENAME SID TO GROUP_ID;
ALTER TABLE GROUPPERMISSION RENAME MASK TO ACCESS_LEVEL;

ALTER TABLE GROUPPERMISSION RENAME CONSTRAINT ACLGROUPPERMISSION_PKEY TO GROUPPERMISSION_PKEY;
ALTER TABLE GROUPPERMISSION RENAME CONSTRAINT ACLGROUPPERMISSION_ENTITY_FKEY TO GROUPPERMISSION_POLICY_FKEY;
ALTER TABLE GROUPPERMISSION RENAME CONSTRAINT ACLGROUPPERMISSION_SID_FKEY TO GROUPPERMISSION_GROUP_FKEY;

ALTER TABLE USERGROUP RENAME USERID TO USER_ID;
ALTER TABLE USERGROUP RENAME GRPID TO GROUP_ID;

ALTER TABLE USERAPPLICATION RENAME USERID TO USER_ID;
ALTER TABLE USERAPPLICATION RENAME APPID TO APPLICATION_ID;

ALTER TABLE GROUPAPPLICATION RENAME GRPID TO GROUP_ID;
ALTER TABLE GROUPAPPLICATION RENAME APPID TO APPLICATION_ID;

ALTER TABLE TOKENAPPLICATION RENAME TOKENID TO TOKEN_ID;
ALTER TABLE TOKENAPPLICATION RENAME APPID TO APPLICATION_ID;

