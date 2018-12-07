Ego Design Notes
================

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

