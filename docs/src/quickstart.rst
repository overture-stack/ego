Quick Start
===========

The goal of this quick start is to get a working application quickly up and running.

Step 1 - Setup Database
-----------------------

1. Install Postgres
2. Create a Database: ego with user postgres and empty password
3. Execute SQL Script to setup tables.

Step 2 - Run
------------

EGO currently supports three Profiles:

- default: Use this to run the most simple setup. This lets you test various API endpoints without a valid JWT in authorization header.
- auth: Run this to include validations for JWT.
- secure: Run this profile to enable https

Run using Maven. Maven can be used to prepare a runnable jar file, as well as the uber-jar for deployment:

.. code-block:: bash

  $ mvn clean package


To run from command line with maven:

.. code-block:: bash

  $ mvn spring-boot:run
