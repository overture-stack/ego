.. Ego documentation master file, created by
   sphinx-quickstart on Fri Mar 16 14:17:55 2018.
   You can adapt this file completely to your liking, but it should at least
   contain the root `toctree` directive.

===============================
Welcome to Ego's documentation!
===============================

Ego is an OAuth2 based Authorization Provider microservice. It is designed to allow users to log in with social logins such as Google and Facebook.

Users, Groups, and Applications can be managed through Ego and allows for stateless authorization of user actions by client applications through the issuing of JWT Bearer tokens and the publishing of a public key for the verification of tokens. 

.. image:: ego-arch.png

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
   src/technology


Indices and tables
==================

* :ref:`genindex`
* :ref:`modindex`
* :ref:`search`
