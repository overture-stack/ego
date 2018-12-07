User Authorization Tokens
=========================
Authorization concerns what a user is *allowed to do*.

Ego's User Authorization tokens are random numbers that Ego issues to users
so they can interact with Ego-aware applications with a chosen level of authority. 

Each token is a unique secret password that is associated with a specific user, permissions, and optionally, an allowed set of applications.  

Unlike passwords, Authorization tokens automatically expire, and they can be 
revoked if the user suspects that they have been compromised. 
 
The user can then use their token with Ego-authorized applications as proof
of who they are and what they are allowed to do. Typically, the user will
configure a client program (such as SING, the client program used with SONG, the ICGC Metadata management service) with their secret token, and the program
will then operate with the associated level of authority. 

In more detail, when an Ego-aware application wants to know if it authorized to do something on behalf of a given user, it just sends their user authorization token to Ego, and gets back the associated information about who the user is (their user id), and what they are allowed to do (the permissions associated with their token).  If the permissions that the user have include the permission the application wants, the application know it is authorized to perform the requested service on behalf of the user.  
