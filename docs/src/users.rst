Ego for End Users
=================
.. image:: EndUser.png

1) User uses a web service/other program to securely log into Ego using Oauth 2.0, using their Google/Facebook account.
2) Google/Facebook confirm the user's identity, and send Ego a confirmation.
3) Ego looks up the user's data, and sends back an authorization token.
4) The end user's program uses their authorization token to tell the user
   what their available applications and permissions are. 
5) The end user chooses which applications and which permissions they wish to
   use for a given token, and use their program to request an authorization 
   token from Ego.
6) Ego creates a token (random number) associated with their information, and
   returns it to them. 
7) The user then uses that token to interact with Ego-aware applications.
   When the application wants to know if the user is allowed to do something,
   it sends their token to ego, which replies back with who the user is and
   what their token allows them to do. 

   If the permissions that are available include the permissions that are 
   required, the application knows it is authorized to do whatever it is 
   being asked to do.


