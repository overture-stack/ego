# ego

An OAuth 2.0 Authorization service that supports multiple OpenID Connect Identity Providers

[![Build Status](https://travis-ci.org/overture-stack/ego.svg?branch=master)](https://travis-ci.org/overture-stack/ego)
[![CircleCI](https://circleci.com/gh/overture-stack/ego/tree/develop.svg?style=svg)](https://circleci.com/gh/overture-stack/ego/tree/develop)

## Build and Run

Maven can be used to prepare a runnable jar file, as well as the uber-jar for deployment:
```bash
$ mvn clean package
```

To run from command line with maven:
```bash
$ mvn spring-boot:run
```

## API

Interactive documentation of the API is provided using Swagger UI.

When run locally this can be found at:  [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)


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
            "name": "Demo.User@example.com",
            "email": "Demo.User@example.com",
            "status": "Approved",
            "firstName": "Demo",
            "lastName": "User",
            "createdAt": null,
            "lastLogin": null,
            "preferredLanguage": null,
            "roles": ["ADMIN"]
        }
    }
}
.
[signature]
```

#### Notes
* "aud" field can contain one or more client IDs. This field indicates the client services that are authorized to use this JWT.
* "groups" will differ based on the domain of client services - each domain of service should get list of groups from that domain's ego service
