# Ego - Authentication and Authorization Microservice

[<img hspace="5" src="https://img.shields.io/docker/pulls/overture/ego?style=for-the-badge">](#docker-setup)
[<img hspace="5" src="https://img.shields.io/badge/chat-on--slack-blue?style=for-the-badge">](http://slack.overture.bio)
[<img hspace="5" src="https://img.shields.io/badge/License-gpl--v3.0-blue?style=for-the-badge">](https://github.com/overture-stack/ego/blob/develop/LICENSE)
[<img hspace="5" src="https://img.shields.io/badge/Code%20of%20Conduct-2.1-blue?style=for-the-badge">](code_of_conduct.md)

<div>
<img align="right" width="66vw" vspace="5" src="icon-ego.png" alt="ego-logo" hspace="30"/>
</div>

Biomedical data requires secure protocols for authenticating users and authorizing the information and applications those users can access. [Ego](https://www.overture.bio/products/ego/) addresses this by facilitating user registration and providing a secure permission management system. 
An [Ego UI tool](https://github.com/overture-stack/ego-ui) was also developed to make these services accessible to all collaborators.

<!--Blockqoute-->

</br>

> 
> <div>
> <img align="left" src="ov-logo.png" height="90" hspace="0"/>
> </div>
> 
> *Ego is a vital service within the [Overture](https://www.overture.bio/) research software ecosystem. With our genomics data management solutions, scientists can significantly improve the lifecycle of their data and the quality of their research. See our [related products](#related-products) for more information on what Overture can offer.*
> 
> 

<!--Blockqoute-->

## Technical Specifications

- Written in JAVA 
- Uses well-known single-sign-on identity providers such as Google, GitHub, LinkedIn and ORCiD.
- [OAuth 2.0](https://oauth.net/2/) and [OpenID Connect](https://auth0.com/docs/authenticate/protocols/openid-connect-protocol) compliant
- Developed with [Sprint Boot](https://spring.io/projects/spring-boot) and [Spring Security Frameworks](https://spring.io/projects/spring-security)
- Scalable with [JSON Web Tokens (JWT)](https://jwt.io/)
- For more information visit our [wiki](https://www.overture.bio/documentation/ego/)

## Documentation

- See our Developer [wiki](https://github.com/overture-stack/ego/wiki)
- For our user installation guide see our website [here](https://www.overture.bio/documentation/ego/installation/)
- For administrative guidance see our website [here](https://www.overture.bio/documentation/ego/user-guide/admin-ui/)

## Docker Setup

This is a step-by-step guide for setting up a dockerized version of Ego.

1. Set up a google oauth client app. [See here](https://www.overture.bio/documentation/ego/installation/prereq/#google) for more details

- *Note it may take **5 minutes to a few hours** for settings to take effect*

2. Clone or Download the repository and update the  ```docker-compose-all.yml``` file with your client id and secret

```
spring.security.oauth2.client.registration.google.clientId : "<insert-provided-client-Id>"
spring.security.oauth2.client.registration.google.clientSecret: "<insert-provided-clientSecret>"
```

3. Open Docker desktop and then run the following command from your CLI

```
docker-compose -f docker-compose-all.yml up 
```

4. Ego requires seed data to authorize the Ego UI as a client using the following command

*Alternatively if you have ```Make``` installed you can run  ```make init-db```*
```
docker exec ego-postgres-1  psql -h localhost -p 5432 -U postgres -d ego --command "INSERT INTO EGOAPPLICATION (name, clientId, clientSecret, redirectUri, description, status, errorredirecturi) VALUES ('ego ui', 'ego-ui', 'secret', 'http://localhost:8080/', '...', 'APPROVED', 'http://localhost:8080/error') on conflict do nothing"
```

5. You can now access the Ego UI through ```http://localhost:8080/ego-ui```
- This will require your google sign in 
- Once signed in you will have access to the admin dashboard
- The Ego swagger ui can be located at ```http://localhost:8080/swagger-ui.html```

## Support & Contributions

- Filing an [issue](https://github.com/overture-stack/ego/issues)
- Making a [contribution](CONTRIBUTING.md)
- Connect with us on [Slack](http://slack.overture.bio)
- Add or Upvote a [feature request](https://github.com/overture-stack/ego/issues?q=is%3Aopen+is%3Aissue+label%3Anew-feature+sort%3Areactions-%2B1-desc)

## Related Products 

<div>
  <img align="right" alt="Overture overview" src="https://www.overture.bio/static/124ca0fede460933c64fe4e50465b235/a6d66/system-diagram.png" width="45%" hspace="5">
</div>

Overture is an ecosystem of research software tools, each with narrow responsibilities, designed to address the adapting needs of genomics research. 

The DMS is a fully functional and customizable data portal built from a packaged collection of Overtures microservices. For more information on our DMS, read our [DMS documentation](https://www.overture.bio/documentation/dms/).

<!--Read our architecture overview to see how these services relate-->

See the links below for additional information on our other research software tools:

</br>

|Product|Description|
|---|---|
|[Ego](https://www.overture.bio/products/ego/)|An authorization and user management service|
|[Ego UI](https://www.overture.bio/products/ego-ui/)|A UI for managing EGO authentication and authorization services|
|[Score](https://www.overture.bio/products/score/)| Transfer data quickly and easily to and from any cloud-based storage system|
|[Song](https://www.overture.bio/products/song/)|Catalog and manage metadata of genomics data spread across cloud storage systems|
|[Maestro](https://www.overture.bio/products/maestro/)|Organizing your distributed data into a centralized Elasticsearch index|
|[Arranger](https://www.overture.bio/products/arranger/)|Organize an intuitive data search interface, complete with customizable components, tables, and search terms|

