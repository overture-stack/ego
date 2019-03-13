-- Create new types
CREATE TYPE statustype AS ENUM('Approved', 'Rejected', 'Disabled', 'Pending');
CREATE TYPE usertype AS ENUM('USER', 'ADMIN');
CREATE TYPE languagetype AS ENUM('English', 'French', 'Spanish');

-- Update Users status column to be of type statustype
ALTER TABLE egouser DROP CONSTRAINT egouser_status_check;
ALTER TABLE egouser ALTER COLUMN status TYPE statustype USING status::statustype;
ALTER TABLE egouser ALTER COLUMN status SET NOT NULL;
ALTER TABLE egouser ALTER COLUMN status SET DEFAULT 'Pending';

-- Update Applications status column to be of type statustype
ALTER TABLE egoapplication DROP CONSTRAINT egoapplication_status_check;
ALTER TABLE egoapplication ALTER COLUMN status TYPE statustype USING status::statustype;
ALTER TABLE egoapplication ALTER COLUMN status SET NOT NULL;
ALTER TABLE egoapplication ALTER COLUMN status SET DEFAULT 'Pending';

-- Update Group status column to be of type statustype
ALTER TABLE egogroup DROP CONSTRAINT egogroup_status_check;
ALTER TABLE egogroup ALTER COLUMN status TYPE statustype USING status::statustype;
ALTER TABLE egogroup ALTER COLUMN status SET NOT NULL;
ALTER TABLE egogroup ALTER COLUMN status SET DEFAULT 'Pending';

-- Change usertype to type since it is redundant in context of an user
ALTER TABLE egouser RENAME usertype TO type;

-- Change the type of column type to usertype enum
ALTER TABLE egouser ALTER COLUMN type TYPE usertype USING type::usertype;
ALTER TABLE egouser ALTER COLUMN type SET NOT NULL;
ALTER TABLE egouser ALTER COLUMN type SET DEFAULT 'USER';

-- Change the type of column preferredlanguage to languagetype enum
ALTER TABLE egouser DROP CONSTRAINT egouser_preferredlanguage_check;
ALTER TABLE egouser ALTER COLUMN preferredlanguage TYPE languagetype USING preferredlanguage::languagetype;

-- Change applicationtype to type since it is redundant in context of an application
ALTER TABLE egoapplication RENAME applicationtype TO type;

