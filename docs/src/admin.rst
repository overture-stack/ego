Ego for Administrators
======================
To administer Ego, the admin must:

(1) Install Ego.

(2) Inserts a new user with the admin's Oauth Id into the "egousers" table, with role ADMIN.

(3) Whenever a developer creates a new Ego-aware application
      (a) create a new application in Ego with the client_id and password.
      (b) create new policies with the new policy names
      (c) assign permissions to users/groups to permit/deny them access to the
          new application

(4) Create or delete groups, assign user/group permissions, expire tokens, etc.
    as necessary. 
    
    For example, an administrator might want to:

     - Create a new group called "QA", whose members are all the people in the "QA department"
     - Create a group called "Access Denied" with access level "DENY" set for every policy in Ego 
     - Grant another user administrative rights (role ADMIN) 
     - Add a former employee to the group "AccessDenied", and revoke all of their active tokens. 
     - In general, manage permissions and access controls within Ego.
