TRUNCATE public.egouser CASCADE;

INSERT INTO public.egouser (name, email, role, firstname, lastname, createdat, lastlogin, status, preferredlanguage) VALUES ('jondoe', 'jon.doe@nih.gov', 'USER', 'jondoe', 'Doe', '2017-01-15 04:35:55', '2016-12-15 23:20:51', 'Approved', null);
INSERT INTO public.egouser (name, email, role, firstname, lastname, createdat, lastlogin, status, preferredlanguage) VALUES ('patriciag', 'patricia.garner@nih.gov', 'USER', 'patriciag', 'Garner', '2017-01-15 04:35:55', '2016-12-15 23:20:51', 'Approved', null);
INSERT INTO public.egouser (name, email, role, firstname, lastname, createdat, lastlogin, status, preferredlanguage) VALUES ('robsmith', 'robert.smith@nih.gov', 'ADMIN', 'robsmith', 'Smith', '2017-01-15 04:35:55', '2016-12-15 23:20:51', 'Approved', null);
INSERT INTO public.egouser (name, email, role, firstname, lastname, createdat, lastlogin, status, preferredlanguage) VALUES ('VFERRETTI', 'vincent.ferretti@oicr.on.ca', 'ADMIN', 'VFERRETTI', 'Ferretti', '2017-01-15 04:35:55', '2016-12-15 23:20:51', 'Approved', null);
