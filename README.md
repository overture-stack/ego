<h1 align="center"> EGO </h1><br>

<p align="center">
  A scalable stateless Authorization Service for Federated Identities including Google and Facebook
</p>

<p align="center">
  <a href="http://www.overture.bio/products/ego" target="_blank"><img alt="General Availability" title="General Availability" src="http://www.overture.bio/img/progress-horizontal-GA.svg" width="320" /></a>
</p>

[![BrowserStack Status](https://www.browserstack.com/automate/badge.svg?badge_key=U3dLZnRFNWI2MWNFY2NGcXVtVTB3WDcyU2dPVjlVeEFYUEdxUnpYZlhrUT0tLTFzY0taYTA0MVFEa3ErNkRZdTBRWVE9PQ==--690f89a41a0eedf7b4975bd7df2eac162e04e775% )](https://www.browserstack.com/automate/public-build/U3dLZnRFNWI2MWNFY2NGcXVtVTB3WDcyU2dPVjlVeEFYUEdxUnpYZlhrUT0tLTFzY0taYTA0MVFEa3ErNkRZdTBRWVE9PQ==--690f89a41a0eedf7b4975bd7df2eac162e04e775%)
[![Slack](http://slack.overture.bio/badge.svg)](http://slack.overture.bio)

## Table of Contents
- [Documentation](#documentation)
- [Introduction](#introduction)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Quick Start](#quick-start)
- [Development Install](#development-install)
  - [Step 1 - Setup Database](#step-1---setup-database)
  - [Step 2 - Run](#step-2---run)
- [Tech Specifications](#tech-specification)
- [Usage](#usage)
- [Shoutouts](#shoutouts)
  - [Browserstack](#browserstack)

## Documentation

Explore documentation with the Ego [Read the Docs](https://ego.readthedocs.io/en/latest/).

## Introduction

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Authorization Service built to provide Single Sign On for various microservices in an application. EGO works with Identity Providers such as Google, Facebook to provide social logins in the application. EGO provides stateless authorization using [JWT (JSON Web Tokens)](https://jwt.io/) and can scale very well to a large number of users.

Interactive documentation of the API is provided using Swagger UI.

When run locally this can be found at: [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html)

<p align="center">
    <img alt="arch" title="EGO Architecture" src="/docs/ego-arch.png" width="480">
</p>
<p align="center">
  EGO Architecture
</p>

## Features

Here are some of the features of EGO:

- Single Sign on for microservices
- User-authentication through Federated Identities such as Google, Facebook, Github (Coming Soon), ORCID (Coming Soon)
- Uses JWT(Json Web Tokens) for Authorization Tokens
- Provides ability to create permission lists for users and/or groups on user-defined permission entities
- Built using well established Frameworks - Spring Boot, Spring Security

## Tech Stack

The application is written in JAVA using Spring Boot and Spring Security Frameworks.

- [Spring Security](https://projects.spring.io/spring-security/)
- [JWT (JSON Web Tokens)](https://jwt.io/): This project uses [jjwt library](https://github.com/jwtk/jjwt) for JWT related features.
- [OpenID Connect](http://openid.net/connect/)

## Quick Start

The goal of this quick start is to get a working application quickly up and running.

Set the `API_HOST_PORT` where ego is to be run, then run docker compose:

```
API_HOST_PORT=8080 docker-compose up -d
```

Ego should now be deployed locally with the swagger ui at
`http://localhost:8080/swagger-ui.html`

## Development Install

### Step 1 - Setup Database

1. Install Postgres
2. Create a Database: ego with user postgres and empty password
3. Run the migrations found here: [SQL Script](/src/main/resources/flyway/sql/) to setup tables.

#### Database Migrations with Flyway

Database migrations and versioning is managed by [flyway](https://flywaydb.org/).

1. Download the flyway cli client here: [flyway-commandline](https://flywaydb.org/download/community)
2. Unpack the client in a directory of your choosing
3. Execute the flyway client pointing it to the configuration and migration directories in this repository.

Get current version information:

```bash
./fly
```

Run outstanding migrations:

```bash
./fly migrate
```

To see the migration naming convention, [click here.](https://flywaydb.org/documentation/migrations#naming)

### Step 2 - Run

- EGO currently supports three Profiles:
  - default: Use this to run the most simple setup. This lets you test various API endpoints without a valid JWT in
    authorization header.
  - auth: Run this to include validations for JWT.
  - secure: Run this profile to enable https
- Run using Maven. Maven can be used to prepare a runnable jar file, as well as the uber-jar for deployment:

```bash
$ mvn clean package
```

To run from command line with maven:

```bash
$ mvn spring-boot:run
```

#### Tech Specifications

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
            "createdAt": "2017-11-23 10:24:41",
            "lastLogin": "2017-11-23 11:23:58",
            "preferredLanguage": null,
            "roles": ["ADMIN"],
            "groups": ["GroupOne", "GroupTwo"],
            "permissions": ["Study001.WRITE", "Study002.DENY"]
        }
    }
}
.
[signature]
```

#### Notes
- "aud" field can contain one or more client IDs. This field indicates the client services that are authorized to use this JWT.
- "groups" will differ based on the domain of client services - each domain of service should get list of groups from that domain's ego service.
- "permissions" will differ based on domain of client service - each domain of service should get list of permissions from that domain's ego service.
- Unit Tests using testcontainers will also run flyway migrations to ensure database has the correct structure

## Usage

### Application Authentication

Applications can be added to EGO with a client ID/Secret pair. The ID and Secret can be used to authenticate with EGO to retrieve a JWT.

An application JWT will not have `roles` but will list the `groups` the application is associated with. Other applications are responsible for controlling authorization for applications based on the content of their signed JWT.

#### Register application

To register an application with EGO make a request as documented at [/swagger-ui.html#!/application-controller/createUsingPOST](/swagger-ui.html#!/application-controller/createUsingPOST)

This request must have an `ADMIN` role JWT in the Authorization field.

Keep the client ID/Secret pair in a secret place, for use by the application only. Do not make these values visible in the browser or in your code base.

#### Authenticate

Authentication uses the oauth client_credentials flow. This can be handled out-of-the-box by many REST clients (ex. [Insomnia](https://insomnia.rest/)).

The Authenticate request details, to recreate manually:

- `POST` request to `https://{{ego-domain}}/oauth/token`
- Body content, content-type: `x-www-form-urlencoded`
  - `grant_type:client_credentials`
  - `client_id:{{application's client id}}`
  - `client_secret:{{application's client secret}}`

curl example:

```curl -X POST \
  http://localhost:8081/oauth/token \
  -H 'Content-Type: application/x-www-form-urlencoded' \
  -d 'grant_type=client_credentials&client_id=my-app-id&client_secret=secretpassword'
```

## Shoutouts

### Browserstack
Many thanks to [Browserstack](https://www.browserstack.com/) for giving our test capabilities a powerup!
![Browserstack](https://p14.zdusercontent.com/attachment/1015988/qyPFNKIZXCbr4qKjd5oxrayZc?token=eyJhbGciOiJkaXIiLCJlbmMiOiJBMTI4Q0JDLUhTMjU2In0..-yKqMwgZKdCDJZmW2kcVYw.IKGbK6GBbU3uZ3B7Vapw8uZeQ-uhXDV9kANtz5OOOBl0Ceg6Oi1gS5wqBnStOsCKgb3CibgGIrYjk-odWPwaL9Ei0u3OIDuBldkxF6aJ6eGtC9G4LfbDLGtOnYkUiANvx5HNPb7HZa3QyivKxCcX_MjO5U01F0WbmJajfYBsFVHHLtO0dBqFz-eWZMmLB0yfjZEaVPAUfLk9H1TO4c6Vw91Or29FrzaoGDQmvmcP7Pg00LMoxuaLxGJuuOiUlEe6OunidzxRd_elUZxMJ_caonvHEjSCkq_yHilG67tGewY.IV6Qg9p5vE0TGk59pqZtRg)

