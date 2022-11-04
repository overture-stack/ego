<!--Ego ReadMe-->

<br />

<p align="center"><a href="https://www.overture.bio/" target="_blank"><img src="Overture_logo.png" width="66%"></a></p>

<br />

<!-- Replace slack with discourse once setup -->

[<img src="https://img.shields.io/badge/chat-on--slack-blue">](http://slack.overture.bio) 
[<img src="https://img.shields.io/badge/License-gpl--v3.0-blue">](https://github.com/overture-stack/ego/blob/develop/LICENSE)

## Worry Less Science More

In this Overture repository, we (the [OICR Genome Informatics Team](https://softeng.oicr.on.ca/team/)) develop [Ego](https://www.overture.bio/products/ego/), our open-source authentication and authorization microservice. [Overture](https://www.overture.bio/) is an ensemble of modular solutions for big-data genomic science. Our core products work in concert to manage, explore and visualize molecular and clinical data. Visit our [website](https://www.overture.bio/) for more information on what Overture offers, and check out our other projects on [GitHub](https://github.com/overture-stack/).

## Ego - Secure Authentication & Authorization

<p align="center"><img alt="Ego in action" src="https://www.overture.bio/static/screenshot-21fc2cfc0ac1c3fd9bd7e62196477554.png"></p>

Access to sensitive and valuable information requires complex and secure methods to verify users and authorize what data and applications they are allowed to access. Ego simplifies user management by providing a secure system to authenticate and authorize users of your application. Ego uses well-known single-sign-on identity providers like Google, GitHub, LinkedIn and ORCiD in place of managing usernames and passwords. 

Ego is [OAuth 2.0](https://oauth.net/2/) and [OpenID Connect](https://auth0.com/docs/authenticate/protocols/openid-connect-protocol) compliant. It is written in JAVA and uses [Sprint Boot](https://spring.io/projects/spring-boot) and [Spring Security Frameworks](https://spring.io/projects/spring-security). Because it uses [JSON Web Tokens (JWT)](https://jwt.io/) for stateless authorization, it can scale to accommodate many users. See our thorough [documentation](https://www.overture.bio/documentation/ego/) for further details. 

<!--Where is ego being used?-->

## Related Products 

<p align="left"><img alt="Overture overview" src="https://www.overture.bio/static/124ca0fede460933c64fe4e50465b235/a6d66/system-diagram.png"></p>

Overtures' modular architecture allows you to utilize and mix any of our products to fulfill your individual needs. Our core technologies, however, can also work in concert as an end-to-end data management system (DMS) designed to satisfy the needs of modern large-scale genomic research. For more information on our DMS, please see our [DMS documentation](https://www.overture.bio/documentation/dms/).

See the links below for additional information on our other modular solutions:

|Product|Description|
|---|---|
|[Ego](https://www.overture.bio/products/ego/)|A stateless authorization and user management service|
|[Score](https://www.overture.bio/products/score/)| Transfer data quickly and easily to and from any cloud-based storage system|
|[Song](https://www.overture.bio/products/song/)|Quickly and reliably track genomic metadata scattered across multiple Cloud storage systems|
|[Maestro](https://www.overture.bio/products/maestro/)|Organizing your distributed data into one index|
|[Arranger](https://www.overture.bio/products/arranger/)|Organize an intuitive data search interface, complete with customizable components, tables, and search terms|

<!--Additional programs?-->

## Table of Contents

- [Quick Start](#quickstart)
- [Setup](#setup)
- [Usage](#usage)
- [Contribution](#how-to-contribute)
- [Feedback](#feedback)
- [Code of Conduct](#code-of-conduct)
- [License](#license)

## Quick Start

This is a step-by-step guide for setting up a dockerized version of Ego. See our [setup section](#setup) below for a comprehensive setup guide.

**1.** Set up a google oauth client app ([see here](https://www.overture.bio/documentation/ego/installation/prereq/#google)). 

**2.** Clone or Download the repository and update the  ```docker-compose-all.yml``` file with the provided client id and secret.

```
spring.security.oauth2.client.registration.google.clientId : "<insert-provided-client-Id>"
spring.security.oauth2.client.registration.google.clientSecret: "<insert-provided-clientSecret>"
```

**3.** Run docker compose from your CLI

```
docker-compose -f docker-compose-all.yml up 
```

**4.** Ego will require seed data to authorize the Ego UI as a client. 

```
docker exec ego_postgres_1  psql -h localhost -p 5432 -U postgres -d ego --command "INSERT INTO EGOAPPLICATION (name, clientId, clientSecret, redirectUri, description, status, errorredirecturi) VALUES ('ego ui', 'ego-ui', 'secret', 'http://localhost:8080/', '...', 'APPROVED', 'http://localhost:8080/error') on conflict do nothing"
```

*Alternatively if you have ```Make``` installed you can run  ```make init-db```*

**5.** You can now access the Ego UI through ```http://localhost:8080/ego-ui```
- This will require your google sign in 
- Once signed in you will have access to the admin dashboard (image above).
- The Ego swagger ui can be located at ```http://localhost:8080/swagger-ui.html```

## Setup

Please see the documentation linked below:

- [Setup Prerequisites](https://www.overture.bio/documentation/ego/installation/prereq/)
- [Installation](https://www.overture.bio/documentation/ego/installation/installation/)
- [Configuration](https://www.overture.bio/documentation/ego/installation/configuration/)
- [Authentication](https://www.overture.bio/documentation/ego/installation/authentication/)

## Usage

Please see the documentation linked below:

- [Using the Admin UI](https://www.overture.bio/documentation/ego/user-guide/admin-ui/)
- [Using the API](https://www.overture.bio/documentation/ego/user-guide/api/)

## Contribute

* [Making a Contribution](CONTRIBUTING.md)
* [Filing an issue](https://github.com/overture-stack/ego/issues)

## Feedback

* Connect with us on [Slack](http://slack.overture.bio)
* [Upvote](https://github.com/overture-stack/ego/issues?q=is%3Aopen+is%3Aissue+label%3Anew-feature+sort%3Areactions-%2B1-desc) feature requests

## Code of Conduct

&emsp; [![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](code_of_conduct.md)

## License

Licensed under the [GNU Lesser General Public License v3.0](LICENSE.txt) license.
