CREATE TABLE REFRESHTOKEN (
    id UUID PRIMARY KEY,
    user_id UUID UNIQUE NOT NULL,
    jti UUID UNIQUE NOT NULL,
    issuedate TIMESTAMP NOT NULL,
    expirydate TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES EGOUSER(id)
);