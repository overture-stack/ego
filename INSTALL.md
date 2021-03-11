# Installation Guide

## Prerequisites 
On the platform of your choice (we recommend Linux) please ensure you have the following installed:
* Postgresql 
    * Ensure you have a user you can use that has the ability to apply database migrations to the Ego database and to create extensions.
* Java Runtime Environment version 11.
* A configured OAuth2 OIDC provider with a clientId and clientSecret. (For example Google).
* The UI ready to be hosted or already hosted somewhere. 

## Installation

### Download Ego

You can download the latest version of Ego from [here](https://artifacts.oicr.on.ca/artifactory/dcc-release/bio/overture/ego/[RELEASE]/ego-[RELEASE]-dist.tar.gz). 

Example using curl:
```shell
curl  https://artifacts.oicr.on.ca/artifactory/dcc-release/bio/overture/ego/[RELEASE]/ego-[RELEASE]-dist.tar.gz -o ego-dist.tar.gz
```

### Extract 
Once downloaded, extract the distribution.

```shell
tar zxvf ego-dist.tar.gz
```

This should create a folder with the name of `ego-<version>` where `<version>` is the version number of the release. We recommend moving Ego out of you home directory, to a directory like `/srv`. You may need to use elevated privileges to do this.

```shell
$ sudo mv ego-4.1.0 /srv/
$ ls -l /srv/
total 4
drwxrwxr-x 8 ubuntu ubuntu 4096 Mar 11 18:51 ego-4.1.0
```

We also recommend creating a symlink with the name of ego-current should you ever want to update or rollback to previous version of Ego while maintaining one place to look at for running and configuring. 

```shell
/srv$ sudo ln -sf ego-4.1.0 ego-current
/srv$ ls -la
total 12
drwxr-xr-x  3 root   root   4096 Mar 11 18:56 .
drwxr-xr-x 19 root   root   4096 Mar 11 18:14 ..
drwxrwxr-x  8 ubuntu ubuntu 4096 Mar 11 18:51 ego-4.1.0
lrwxrwxrwx  1 root   root      9 Mar 11 18:56 ego-current -> ego-4.1.0
```

### Database Configuration

The directory structure inside of the Ego directory is self explainatory: 

```shell
/srv/ego-current$ ls -l
total 24
drwxrwxr-x 2 ubuntu ubuntu 4096 Mar 11 18:51 bin
drwxrwxr-x 2 ubuntu ubuntu 4096 Mar 11 18:51 cert
drwxr-xr-x 2 ubuntu ubuntu 4096 Mar 11 17:03 conf
drwxrwxr-x 2 ubuntu ubuntu 4096 Mar 11 18:51 exec
drwxr-xr-x 2 ubuntu ubuntu 4096 Mar 11 17:03 lib
drwxr-xr-x 2 ubuntu ubuntu 4096 Mar 11 17:03 logs
```

We want to navigate into the `conf` directory to edit the `application.yml` file.

```shell
vim conf/application.yml
```

First thing we want to edit is the `spring.datasource` section replacing `<ego_db>`, `<db_user>`, and `<db_pass>` with the values you have configured in your postgresql instance:
```yml
# Datasource
spring.datasource:
  driver-class-name: org.postgresql.Driver
  url: jdbc:postgresql://localhost:5432/<ego_db>?stringtype=unspecified

  username: <db_user>
  password: <db_pass>
  max-active: 10
  max-idle: 1
  min-idle: 1
```

Next, as we have not applied any database migrations, we will want to enable flyway to automatically apply outstanding migrations on startup. Look for the `spring.flyway.enabled` setting and set it to `true`. Also, we will need to tell flyway where to find the migrations. As we are using the built in migrations, we can add: `locations: classpath:flyway/sql,classpath:db.migration`. 

```yml
spring:
  flyway:
    enabled: true # SET THIS TO TRUE, DEFAULT IS FALSE
    user: <privileged_user>
    password: <privileged_user_password>
    locations: classpath:flyway/sql,classpath:db.migration
```

As the migration requires elevated privileges to create extensions within postgresql, feel free to use a separate user for running them. 

### First Startup
Now we are ready to start Ego and initialize the database. We will use the service wrapper `bin/ego` to start/stop/restart Ego.

```shell
/srv/ego-current$ bin/ego 
Usage: bin/ego [ console | start | stop | restart | condrestart | status | install | remove | dump ]

Commands:
  console      Launch in the current console.
  start        Start in the background as a daemon process.
  stop         Stop if running as a daemon or in another console.
  restart      Stop if running and then start.
  condrestart  Restart only if already running.
  status       Query the current status.
  install      Install to start automatically when system boots.
  remove       Uninstall.
  dump         Request a Java thread dump if running.

```
Starting it up for the first time:
```shell
/srv/ego-current$ bin/ego start
Starting EGO Server...
Waiting for EGO Server......
running: PID:11994
```

If we look at the logs we should seem something like the following: 
```shell
$ /srv/ego-current$ tail -f logs/wrapper.20210311.log 
INFO   | jvm 1    | 2021/03/11 19:35:02 | 2021-03-11 19:35:02,492 [WrapperSimpleAppMain] INFO  o.s.b.w.e.t.TomcatWebServer - Tomcat started on port(s): 8081 (http) with context path ''
INFO   | jvm 1    | 2021/03/11 19:35:02 | 2021-03-11 19:35:02,492 [WrapperSimpleAppMain] INFO  o.s.b.w.e.t.TomcatWebServer - Tomcat started on port(s): 8081 (http) with context path ''
INFO   | jvm 1    | 2021/03/11 19:35:02 | 2021-03-11 19:35:02,497 [WrapperSimpleAppMain] INFO  b.o.e.AuthorizationServiceMain - Started AuthorizationServiceMain in 22.659 seconds (JVM running for 24.385)
```

## Auth Setup
Now that Ego is up and running we want to configure the first user and application that can use Ego for Authorization.

### Identity Provider

For the identity provider of your choosing, find the relevant section in the `application.yml` configuration file and the client id and secret you have configured with that IdP. For example is Google is your IdP:


```yml
google:
  client:
    clientId: <cliend_id>
    clientSecret: <client_secret>
    accessTokenUri: https://www.googleapis.com/oauth2/v4/token
    userAuthorizationUri: https://accounts.google.com/o/oauth2/v2/auth
    clientAuthenticationScheme: form
    scope:
      - email
      - profile
  resource:
    userInfoUri: https://www.googleapis.com/oauth2/v3/userinfo
```

### First Application
Before users can login we need to setup the UI application inside of Ego. This can be done by setting `intialization.enabled` to true and configuring the rest of the settings to match the settings you will use in your UI application.

```yml
initialization:
  enabled: true # Set to true
  applications:
    - name: <name_of_your_ui_app>
      type: CLIENT
      clientId: <client_id_of_ui>
      clientSecret: <some_secret> 
      redirectUri: <url to redirect to in UI, for Ego UI this would be root>
      description: Some description about this application  # optional
```

### First User
Users by default do not have the `ADMIN` role and therefor cannot modify Ego or use the Ego UI. We want to allow the first user to login to be an ADMIN user. We can do that by using the following configuration in `application.yml`:

```yml
# Default values available for creation of entities
default:
  user:
    # flag to automatically register first user as an admin
    firstUserAsAdmin: true
    type: ADMIN
    status: APPROVED
```

### Putting it together.
Now that we have updated our config we can restart Ego, and try to login via the UI.
```shell
bin/ego restart
```

## Cleanup

Assuming all is well, and the Ego database is properly configured and the first user and application are working, you will most likely want to disable the initialization of the first application and user. 

Also, if you prefer to manage migrations yourself and not have Ego automatically apply them when you update Ego, disable the flyway migration as well. 