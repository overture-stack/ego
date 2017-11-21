/* WARNING: Clears all data in the EgoGroup Table

Clears the EgoGroup table and insert 5 sample groups (4 Approved, 1 Pending)
*/
TRUNCATE public.egogroup CASCADE;

INSERT INTO egogroup (grpName, status, description) VALUES ('XYZ Cancer Research Institute', 'Approved', 'Sample group for elite cancer researchers');
INSERT INTO egogroup (grpName, status, description) VALUES ('Extreme Research Consortium', 'Approved', 'Sample group for generalist researchers');
INSERT INTO egogroup (grpName, status, description) VALUES ('Healthcare Providers Anonymous', 'Approved', 'Sample group for patient care specialist');
INSERT INTO egogroup (grpName, status, description) VALUES ('Pediatric Patient Support Network', 'Approved', 'Sample group for patients and their supporters');
INSERT INTO egogroup (grpName, status, description) VALUES ('Generic Genomics Geniuses', 'Pending', 'Sample group for super-duper smart genetic investigators');