# ego

An OAuth 2.0 Authorization service that supports multiple OpenID Connect Identity Providers

[![Build Status](https://travis-ci.org/overture-stack/ego.svg?branch=master)](https://travis-ci.org/overture-stack/ego)

## Using

* [Spring Security](https://projects.spring.io/spring-security/)
* [JWT (JSON Web Tokens)](https://jwt.io/)
* [OpenID Connect](http://openid.net/connect/)

## JWT 
This project uses [jjwt library](https://github.com/jwtk/jjwt) for JWT related features.

#### Schema

ego JWT will have a similar format as the one described in RFC: [kf-auth-rfc](https://github.com/kids-first/rfcs/blob/master/text/0000-kf-oauth2.md)
An example ego JWT is mentioned below:

```json
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
            "name": "jdoe",
            "first_name": "Jane",
            "last_name": "Doe",
            "roles": ["admin"],
            "groups" : ["researcher", "parent", "CollabUser", "CollabDownloader"],
            "email": "user@gmail.com"
        }
    }
}
.
[signature]
```

#### Notes
* "aud" field can contain one or more client IDs. This field indicates the client services that are authorized to use this JWT.
* "groups" will differ based on the domain of client services - each domain of service should get list of groups from that domain's ego service