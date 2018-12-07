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

How does Ego work?
==================

1. Developers write Ego-aware applications that will grant a given individual access to a service based upon a given named permission or set of permissions. They configure their code to get those permissions from Ego, by calling Ego's "check_token" REST endpoint. 


2. An Ego admin user configures Ego with permission settings for these applications for users or groups of users.


3. An Ego user requests a user authorization token to grant some (or all) of their available permissions to one or more of their allowed Ego-aware applications.


4. The Ego user uses the requests a service from one of their Ego-aware applications, and sends it the secret token as proof of who they are and what they're authorized to do.

5. The application contacts Ego, which tells it the user and permissions associated with the token. If the user has the permissions that the service requires, the application knows it is authorized to perform the service on behalf of the user.


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
   src/technology
   src/audience
   src/tokens

Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`
