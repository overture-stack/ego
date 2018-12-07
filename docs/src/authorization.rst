Ego Authorization Tokens 
========================

1. Developers write Applications that grant a user access to a given service based upon a given permission or set of permissions (called a scope). 

   They then configure their code to get those permissions from Ego (by calling it's "check_token" REST endpoint). 

2. An Ego admin configures Ego by setting up it's Applications, Users, Groups, Policies, and Permissions.

   These settings specify: 
   a) which Applications Ego will communicate with (and a password for each one) 
   b) which users have access to which set of Applications and Permissions

3. An Ego user can then use Ego to issue an secret authorization token granting some or all of their permissions to 
   some or all of their Ego applications.

4. Next, the Ego user requests a service from one of their Ego applications, and sends it the secret token as 
   proof of who they are and what they're authorized to do.

5. The application then contacts Ego, which tells it the user and permissions associated with the token.

6. The application allows/denies access to the given service based upon those permissions.

.. image:: EndUser.png 
