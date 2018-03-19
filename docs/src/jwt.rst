JSON Web Token
==============

Basics
------

Ego makes use of JSON Web Tokens (JWTs) for providing users with a Bearer token. 

The RFC for JWTs can be found here: https://tools.ietf.org/html/rfc7519

The following is a useful site for understanding JWTs: https://jwt.io/


The following is the structure of an ego JWT:

.. code-block:: guess

  {
      "alg": "HS512"
  }
  .
  {
      "sub": "1234567", 
      "iss": "ego:56fc3842ccf2c1c7ec5c5d14",
      "iat": 1459458458,
      "exp": 1459487258,
      "jti": "56fd919accf2c1c7ec5c5d16",
      "aud": [
          "service1-id",
          "service2-id",
          "service3-id"
      ],
      "context": {
          "user": {
              "name": "Demo.User@example.com",
              "email": "Demo.User@example.com",
              "status": "Approved",
              "firstName": "Demo",
              "lastName": "User",
              "createdAt": "2017-11-23 10:24:41",
              "lastLogin": "2017-11-23 11:23:58",
              "preferredLanguage": null,
              "roles": ["ADMIN"]
          }
      }
  }
  .
  [signature]

Library Support
---------------

The Java JWT library is used in Ego for providing support for encoding, decoding, and validating JWTs: https://github.com/jwtk/jjwt
