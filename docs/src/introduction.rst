==============
Introduction
==============


What is Ego?
=============

`EGO <https://www.overture.bio/products/ego>`_ is an OAuth2 based authentication and authorization management microservice. It allows users to login and authenticate themselves using their existing logins from sites such as Google and Facebook, create and manage authorization tokens, and use those tokens to interact with Ego-aware third party applications which they are authorized for.

OAuth single sign-on means that Ego does not need to manage users and their passwords; and similarly, none of the services that use Ego need to worry about how to manage users, logins, authentication or authorization. The end user simply sends them a token, and the service checks with Ego to learn who the token is for, and what permissions the token grants.
EGO is one of many products provided by `Overture <https://overture.bio>`_ and is completely open-source and free for everyone to use.

.. seealso::

    For additional information on other products in the Overture stack, please visit https://overture.bio

.. _introduction_features:

Features
===========

- Single sign-on for microservices
- User authentication through federated identities such as Google, Facebook, Linkedin, Github (Coming Soon), ORCID (Coming Soon)
- Provides stateless authorization using `JSON Web Tokens (JWT)  <https://jwt.io/>`_
- Can scale very well to large number of users
- Provides ability to create permission lists for users and/or groups on user-defined permission entities
- Standard REST API that is easy to understand and work with
- Interactive documentation of the API is provided using Swagger UI. When run locally, this can be found at : http://localhost:8080/swagger-ui.html
- Built using well established Frameworks - Spring Boot, Spring Security

License
==========
Copyright (c) 2018. Ontario Institute for Cancer Research

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see https://www.gnu.org/licenses.
