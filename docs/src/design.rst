# How Ego Api Tokens Work:

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

#Benefits of This Design:

1. OAuth Single Sign-On means that Ego doesn't need to manage users and their passwords; users don't need a new username or password, and don't need to trust any service other than Google / Facebook.

2. Ego lets users be in charge of the authority they give out; so they can issue secret tokens that are limited to 
   the exact authority level they need to do a given task. 

   Even if a such a token becomes publicly known, it can't grant an outsider accesses to services or permissions 
   that the token doesn't have -- regardless of whether the user has more authority that they could have granted. 

   Tokens also automatically expire (by default, within 24 hours), and if a user suspects that a token may have
   become known to outsiders, they can simply revoke the compromised token, removing all of it's authority, 
   then issue themselves a new secret token, and use it.

3. None of the services that use Ego uses need to manage worry about how to manage users, logins, authentication, 
   or authorization. The end user simply sends them a token, and the service checks with Ego to learn who the 
   token is for, and what permissions the token grants. If the permissions granted don't include the permissions 
   the service needs, it denies access; otherwise, it runs the service for the given user. 

