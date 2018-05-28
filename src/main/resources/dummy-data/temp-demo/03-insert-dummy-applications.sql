
TRUNCATE public.egoapplication CASCADE;

INSERT INTO EGOAPPLICATION (name, clientId, clientSecret, redirectUri, description, status) VALUES ('TCGA-KIRC', 'tcga-kirc', 'tcga-kirc-secret', 'http://google.com', 'TCGA-KIRC', 'Approved');
INSERT INTO EGOAPPLICATION (name, clientId, clientSecret, redirectUri, description, status) VALUES ('TCGA-KIRP', 'tcga-kirp', 'tcga-kirp-secret', 'http://google.com', 'TCGA-KIRP', 'Approved');
INSERT INTO EGOAPPLICATION (name, clientId, clientSecret, redirectUri, description, status) VALUES ('TCGA-KICH', 'tcga-kich', 'tcga-kich-secret', 'http://google.com', 'TCGA-KICH', 'Approved');
INSERT INTO EGOAPPLICATION (name, clientId, clientSecret, redirectUri, description, status) VALUES ('TCGA-GBM',  'tcga-gbm', 'tcga-gbm-secret', 'http://google.com', 'TCGA-GBM', 'Approved');
INSERT INTO EGOAPPLICATION (name, clientId, clientSecret, redirectUri, description, status) VALUES ('TCGA-LGG',  'tcga-lgg', 'tcga-lgg-secret', 'http://google.com', 'TCGA-LGG', 'Approved');
