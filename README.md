# Ego - Identity and Access Management

[<img hspace="5" src="https://img.shields.io/badge/chat-on--slack-blue?style=for-the-badge">](http://slack.overture.bio)
[<img hspace="5" src="https://img.shields.io/badge/License-gpl--v3.0-blue?style=for-the-badge">](https://github.com/overture-stack/ego/blob/develop/LICENSE)
[<img hspace="5" src="https://img.shields.io/badge/Code%20of%20Conduct-2.1-blue?style=for-the-badge">](code_of_conduct.md)

<div>
<img align="right" width="66vw" vspace="5" src="icon-ego.png" alt="ego-logo" hspace="30"/>
</div>

 Ego is an identity and access management microservice that provides secure protocols for the authentication and authorization of users and applications. Ego can be paired with our [Ego UI](https://github.com/overture-stack/ego-ui) making these services accessible for all collaborators.

**Other features and benefits include:**

- Support for popular Single-Sign-On identity providers (Github, ORCiD, and Google)
- Manage access tokens for applications, individual users or groups of users
- Stateless authorization for efficient scalability

<!--Blockqoute-->

</br>

> 
> <div>
> <img align="left" src="ov-logo.png" height="90"/>
> </div>
> 
> *Ego is a core component within the [Overture](https://www.overture.bio/) research software ecosystem. Overture is a toolkit of modular software components made to build into scalable data management systems. See our [related products](#related-products) for more information on what Overture can offer.*
> 
> 

<!--Blockqoute-->

## Technical Specifications

- Written in JAVA 
- [OAuth 2.0](https://oauth.net/2/) and [OpenID Connect](https://auth0.com/docs/authenticate/protocols/openid-connect-protocol) compliant
- Developed with [Sprint Boot](https://spring.io/projects/spring-boot) and [Spring Security Frameworks](https://spring.io/projects/spring-security)
- Leverages [JSON Web Tokens (JWT)](https://jwt.io/)

## Documentation

- For developer documentation, including instructions for running Ego from source read our [GitHub Wiki](https://github.com/overture-stack/ego/wiki)
- For user documentation, including installation, configuration and usage guides, see our websites [Ego documentation](https://www.overture.bio/documentation/ego/)

## Docker Setup

This is a step-by-step guide for setting up a dockerized version of Ego.

1. Set up a google oauth client app. [See here](https://www.overture.bio/documentation/ego/installation/prereq/#google) for more details

- *Note it may take **5 minutes to a few hours** for settings to take effect*

2. Clone or Download the repository and update the  `docker-compose-all.yml` file with your client id and secret

```
spring.security.oauth2.client.registration.google.clientId : "<insert-provided-client-Id>"
spring.security.oauth2.client.registration.google.clientSecret: "<insert-provided-clientSecret>"
```

3. Open Docker desktop and then run the following command from your CLI

```
docker-compose -f docker-compose-all.yml up 
```

4. Ego requires seed data to authorize the Ego UI as a client using the following command

*Alternatively if you have `Make` installed you can run  `make init-db`*
```
docker exec ego-postgres-1  psql -h localhost -p 5432 -U postgres -d ego --command "INSERT INTO EGOAPPLICATION (name, clientId, clientSecret, redirectUri, description, status, errorredirecturi) VALUES ('ego ui', 'ego-ui', 'secret', 'http://localhost:8080/', '...', 'APPROVED', 'http://localhost:8080/error') on conflict do nothing"
```

5. You can now access the Ego UI through `http://localhost:8080/ego-ui`
- This will require your google sign in 
- Once signed in you will have access to the admin dashboard
- The Ego swagger ui can be located at `http://localhost:8080/swagger-ui.html`

## Support & Contributions

- Filing an [issue](https://github.com/overture-stack/ego/issues)
- Making a [contribution](CONTRIBUTING.md)
- Connect with us on [Slack](http://overture-bio.slack.com)
- Add or Upvote a [feature request](https://github.com/overture-stack/ego/issues?q=is%3Aopen+is%3Aissue+label%3Anew-feature+sort%3Areactions-%2B1-desc)

## Related Products 

<div>
  <img align="right" alt="Overture overview" src="https://www.overture.bio/static/124ca0fede460933c64fe4e50465b235/a6d66/system-diagram.png" width="45%" hspace="5">
</div>

Overture is an ecosystem of research software tools, each with narrow responsibilities, designed to address the adapting needs of genomics research. 

All our core microservices are included in the Overture **Data Management System** (DMS). Built from our core collection of microservices, the DMS offers turnkey installation, configuration, and deployment of Overture software. For more information on the DMS, read our [DMS documentation](https://www.overture.bio/documentation/dms/).

See the links below for information on our other research software tools:

</br>

|Software|Description|
|---|---|
|[Ego](https://www.overture.bio/products/ego/)|An authorization and user management service|
|[Ego UI](https://www.overture.bio/products/ego-ui/)|A UI for managing Ego authentication and authorization services|
|[Score](https://www.overture.bio/products/score/)| Transfer data to and from any cloud-based storage system|
|[Song](https://www.overture.bio/products/song/)|Catalog and manage metadata associated to file data spread across cloud storage systems|
|[Maestro](https://www.overture.bio/products/maestro/)|Organizing your distributed data into a centralized Elasticsearch index|
|[Arranger](https://www.overture.bio/products/arranger/)|A search API with reusable UI components that build into configurable and functional data portals|
|[DMS-UI](https://github.com/overture-stack/dms-ui)|A simple web browser UI that integrates Ego and Arranger|
