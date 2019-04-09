/* WARNING: Clears all data in the EgoGroup Table

Clears the EgoGroup table and insert 5 sample groups (4 APPROVED, 1 PENDING)
*/
TRUNCATE public.egogroup CASCADE;

INSERT INTO egogroup (name, status, description) VALUES ('XYZ Cancer Research Institute', 'APPROVED', 'Sample group for elite cancer researchers');
INSERT INTO egogroup (name, status, description) VALUES ('Extreme Research Consortium', 'APPROVED', 'Sample group for generalist researchers');
INSERT INTO egogroup (name, status, description) VALUES ('Healthcare Providers Anonymous', 'APPROVED', 'Sample group for patient care specialist');
INSERT INTO egogroup (name, status, description) VALUES ('Pediatric Patient Support Network', 'APPROVED', 'Sample group for patients and their supporters');
INSERT INTO egogroup (name, status, description) VALUES ('Generic Genomics Geniuses', 'PENDING', 'Sample group for super-duper smart genetic investigators');