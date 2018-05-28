TRUNCATE public.egouser CASCADE;

INSERT INTO public.egouser (name, email, role, firstname, lastname, createdat, lastlogin, status, preferredlanguage) VALUES ('jondoe', 'jon.doe@nih.gov', 'USER', 'jondoe', 'Doe', '2017-01-15 04:35:55', '2016-12-15 23:20:51', 'Approved', null);
INSERT INTO public.egouser (name, email, role, firstname, lastname, createdat, lastlogin, status, preferredlanguage) VALUES ('patriciag', 'patricia.garner@nih.gov', 'USER', 'patriciag', 'Garner', '2017-01-15 04:35:55', '2016-12-15 23:20:51', 'Approved', null);
INSERT INTO public.egouser (name, email, role, firstname, lastname, createdat, lastlogin, status, preferredlanguage) VALUES ('robsmith', 'robert.smith@nih.gov', 'ADMIN', 'robsmith', 'Smith', '2017-01-15 04:35:55', '2016-12-15 23:20:51', 'Approved', null);
INSERT INTO public.egouser (name, email, role, firstname, lastname, createdat, lastlogin, status, preferredlanguage) VALUES ('VFERRETTI', 'vincent.ferretti@oicr.on.ca', 'ADMIN', 'VFERRETTI', 'Ferretti', '2017-01-15 04:35:55', '2016-12-15 23:20:51', 'Approved', null);

TRUNCATE public.egogroup CASCADE;

INSERT INTO egogroup (name, status, description) VALUES ('AWG-KIDNEY', 'Approved', 'AWG Kidney pre-released data');
INSERT INTO egogroup (name, status, description) VALUES ('AWG-BRAIN', 'Approved', 'AWG Brain pre-released data');

TRUNCATE public.egoapplication CASCADE;

INSERT INTO EGOAPPLICATION (name, clientId, clientSecret, redirectUri, description, status) VALUES ('TCGA-KIRC', 'tcga-kirc', 'tcga-kirc-secret', 'http://google.com', 'TCGA-KIRC', 'Approved');
INSERT INTO EGOAPPLICATION (name, clientId, clientSecret, redirectUri, description, status) VALUES ('TCGA-KIRP', 'tcga-kirp', 'tcga-kirp-secret', 'http://google.com', 'TCGA-KIRP', 'Approved');
INSERT INTO EGOAPPLICATION (name, clientId, clientSecret, redirectUri, description, status) VALUES ('TCGA-KICH', 'tcga-kich', 'tcga-kich-secret', 'http://google.com', 'TCGA-KICH', 'Approved');
INSERT INTO EGOAPPLICATION (name, clientId, clientSecret, redirectUri, description, status) VALUES ('TCGA-GBM',  'tcga-gbm', 'tcga-gbm-secret', 'http://google.com', 'TCGA-GBM', 'Approved');
INSERT INTO EGOAPPLICATION (name, clientId, clientSecret, redirectUri, description, status) VALUES ('TCGA-LGG',  'tcga-lgg', 'tcga-lgg-secret', 'http://google.com', 'TCGA-LGG', 'Approved');
