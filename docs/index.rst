.. Ego documentation master file, created by
   sphinx-quickstart on Fri Mar 16 14:17:55 2018.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

===============================
Welcome to Ego's documentation!
===============================

What is Ego?
============
Ego is an OAuth2 based Authentication and Authorization microservice. It lets users log in using their existing logins from sites such as Google and Facebook,
manage authorization tokens, and use those tokens to grant permissions to
Ego-aware third party applications.

.. image:: ego-arch.png

How does it work?
===================
Users, Groups, and Applications can be managed through Ego. Ego issues two 
distinct types of tokens. 

1) Authentication tokens, which are used to verify a user's identity. Authentication tokens are signed JSON Web Tokens (see http://jwt.io) that Ego issues when
a user successfully logs into Ego using their Google or Facebook credentials. 

An authentication token contains all of the information that ego has about a given user, including which groups they are a part of, which applications they are authorized to use , which permissions they have to use those appliactions.

This data current as of the time the token is issued, and the token is 
digitally signed by Ego with a publicly available signing key that applications
have use to verify that an authentication token is valid. Most of Ego's 
REST endpoints require an Ego authentication token to validate the user's
identity before operating on their data. 


2) Authorization tokens, which a user can use selectively authorize 
all or some of their Ego-authorized applications to perform activities 
using a given set of their permissions.  Authorization are random numbers 
that Ego associates with a given user, the list of permissions to grant, 
and optionally, an allowed list of applications that may use those permission.

Using their authorization token, the user can then make a service request from
an Ego-authorized application. The authorization token is a random number that
reveals nothing about who it is from or what credentials it allows unless
the application is authorized to communicate with Ego. If the application
is authorized to communicate, and can send the client_id and password to 
authenticate itself with ego, ego will return back the user id and allowed
permissions to the appliacation. The application will then check to see if the
user has the permissions that the application requires to perform the requested
service, and if so, performs the service on behalf of the user.  

Ego allows the configuration of all these users, permissions, and applications to be managed by special users called "administrators", which are ordinary users who have been assigned the role "ADMIN". Administrators can create or delete users, groups, or applications, assign individual users to groups, create new policies, assign users or groups specific permission settings, and so on; either
directly through Ego's REST API, or using a web UI such as the one being developed at xxxxyyy. 

.. toctree::
   :maxdepth: 2

Installation
============
The easiest way to get up and running is with docker.

  **docker pull overture/ego**

Otherwise, you can build from source. The prerequisites are Java 8 and Maven.

.. code-block:: bash

  git clone https://github.com/overture-stack/ego.git
  cd ego
  mvn clean package


Documentation
=============

.. toctree::
   :maxdepth: 4
  
   src/quickstart
   src/glossary
   src/overview
   src/technology
   src/administration
   src/users

Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`
