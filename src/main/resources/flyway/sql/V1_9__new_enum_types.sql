-- Create new types
CREATE TYPE statustype AS ENUM('APPROVED', 'REJECTED', 'DISABLED', 'PENDING');
CREATE TYPE usertype AS ENUM('USER', 'ADMIN');
CREATE TYPE languagetype AS ENUM('ENGLISH', 'FRENCH', 'SPANISH');

-- Convert the Users status column to be of type statustype
ALTER TABLE egouser DROP CONSTRAINT egouser_status_check;
UPDATE egouser SET status = 'APPROVED' WHERE status = 'Approved';
UPDATE egouser SET status = 'REJECTED' WHERE status = 'Rejected';
UPDATE egouser SET status = 'DISABLED' WHERE status = 'Disabled';
UPDATE egouser SET status = 'PENDING'  WHERE status = 'Pending';
ALTER TABLE egouser ALTER COLUMN status TYPE statustype USING status::statustype;
ALTER TABLE egouser ALTER COLUMN status SET NOT NULL;
ALTER TABLE egouser ALTER COLUMN status SET DEFAULT 'PENDING';

-- Convert the Applications status column to be of type statustype
ALTER TABLE egoapplication DROP CONSTRAINT egoapplication_status_check;
UPDATE egoapplication SET status = 'APPROVED' WHERE status = 'Approved';
UPDATE egoapplication SET status = 'REJECTED' WHERE status = 'Rejected';
UPDATE egoapplication SET status = 'DISABLED' WHERE status = 'Disabled';
UPDATE egoapplication SET status = 'PENDING'  WHERE status = 'Pending' OR status IS NULL;
ALTER TABLE egoapplication ALTER COLUMN status TYPE statustype USING status::statustype;
ALTER TABLE egoapplication ALTER COLUMN status SET NOT NULL;
ALTER TABLE egoapplication ALTER COLUMN status SET DEFAULT 'PENDING';

-- Convert the Group 'status' column to be of type statustype
ALTER TABLE egogroup DROP CONSTRAINT egogroup_status_check;
UPDATE egogroup SET status = 'APPROVED' WHERE status = 'Approved';
UPDATE egogroup SET status = 'REJECTED' WHERE status = 'Rejected';
UPDATE egogroup SET status = 'DISABLED' WHERE status = 'Disabled';
UPDATE egogroup SET status = 'PENDING'  WHERE status = 'Pending';
ALTER TABLE egogroup ALTER COLUMN status TYPE statustype USING status::statustype;
ALTER TABLE egogroup ALTER COLUMN status SET NOT NULL;
ALTER TABLE egogroup ALTER COLUMN status SET DEFAULT 'PENDING';

-- Rename the User 'usertype' column to 'type' since 'usertype' is redundant in the context of a user
ALTER TABLE egouser RENAME usertype TO type;

-- Change the User 'type' column to be of type usertype
ALTER TABLE egouser ALTER COLUMN type TYPE usertype USING type::usertype;
ALTER TABLE egouser ALTER COLUMN type SET NOT NULL;
ALTER TABLE egouser ALTER COLUMN type SET DEFAULT 'USER';

-- Convert the User 'preferredlanguage' column to be of type languagetype
ALTER TABLE egouser DROP CONSTRAINT egouser_preferredlanguage_check;
UPDATE egouser SET preferredlanguage = 'ENGLISH' WHERE preferredlanguage = 'English';
UPDATE egouser SET preferredlanguage = 'FRENCH' WHERE preferredlanguage = 'French';
UPDATE egouser SET preferredlanguage = 'SPANISH' WHERE preferredlanguage = 'Spanish';
ALTER TABLE egouser ALTER COLUMN preferredlanguage TYPE languagetype USING preferredlanguage::languagetype;

-- Rename the Application 'applicationtype' column to 'type' since 'applicationtype' is redundant in the context of an application
ALTER TABLE egoapplication RENAME applicationtype TO type;

-- Add default uuid4 generation to other tables just like for Application and Group
ALTER TABLE egouser ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE policy ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE grouppermission ALTER COLUMN id SET DEFAULT uuid_generate_v4();
ALTER TABLE userpermission ALTER COLUMN id SET DEFAULT uuid_generate_v4();

