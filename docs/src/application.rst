Application Tokens
==================
For security reasons, applications need to be able to prove to Ego that they 
are the legitimate applications that Ego has been configured to work with.

For this reason, every Ego-aware application must be configured in Ego with 
it's own unique client_id and password, and the application must send 
an authentication token with this information to Ego whenever it makes a 
request to get the identity and credentials associated with a user's 
authorization token.

.. image:: application.png
 
