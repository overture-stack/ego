UPDATE egouser SET lastname = '' WHERE lastname IS NULL;
ALTER TABLE egouser ALTER COLUMN lastname SET NOT NULL;
ALTER TABLE egouser ALTER COLUMN lastname SET DEFAULT '';

UPDATE egouser SET firstname = '' WHERE firstname IS NULL;
ALTER TABLE egouser ALTER COLUMN firstname SET NOT NULL;
ALTER TABLE egouser ALTER COLUMN firstname SET DEFAULT '';
