# Installation Guide

## Prerequisites 
On the platform of your choice (we recommend Linux) please ensure you have the following installed:
* Postgresql 
    * Ensure you have a user you can use that has the ability to apply database migrations to the Ego database and to create extensions.
* Java Runtime Environment version 11.
* A configured OAuth2 OIDC provider with a clientId and clientSecret. (For example Google).

## Installation

### Download Ego

You can download the latest version of Ego from [here](https://artifacts.oicr.on.ca/artifactory/dcc-release/bio/overture/ego/[RELEASE]/ego-[RELEASE]-dist.tar.gz). 

Example using curl:
```bash
curl  https://artifacts.oicr.on.ca/artifactory/dcc-release/bio/overture/ego/[RELEASE]/ego-[RELEASE]-dist.tar.gz -o ego-dist.tar.gz
```

### Extract 
Once downloaded, extract the distribution.

```
tar zxvf ego-dist.tar.gz
```

This should create a folder with the name of `ego-<version>` where `<version>` is the version number of the release. We recommend moving Ego out of you home directory, to a directory like `/srv`. You may need to use elevated priveledges to do this.

```
$ sudo mv ego-4.1.0 /srv/
$ ls -l /srv/
total 4
drwxrwxr-x 8 ubuntu ubuntu 4096 Mar 11 18:51 ego-4.1.0
```

We also recommend creating a symlink with the name of ego-current should you ever want to update or rollback to previous version of Ego while maintaining one place to look at for running and configuring. 

```
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

```
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

```
vim conf/application.yml
```

First thing we want to edit is the `spring.datasource` section replacing `<ego_db>`, `<db_user>`, and `<db_pass>` with the values you have configured in your postgresql instance:
```
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

```
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

```
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
```
/srv/ego-current$ bin/ego start
Starting EGO Server...
Waiting for EGO Server......
running: PID:11994
```

If we look at the logs we should seem something like the following: 
```
$ /srv/ego-current$ tail -f logs/wrapper.20210311.log 
INFO   | jvm 1    | 2021/03/11 19:35:02 | 2021-03-11 19:35:02,492 [WrapperSimpleAppMain] INFO  o.s.b.w.e.t.TomcatWebServer - Tomcat started on port(s): 8081 (http) with context path ''
INFO   | jvm 1    | 2021/03/11 19:35:02 | 2021-03-11 19:35:02,492 [WrapperSimpleAppMain] INFO  o.s.b.w.e.t.TomcatWebServer - Tomcat started on port(s): 8081 (http) with context path ''
INFO   | jvm 1    | 2021/03/11 19:35:02 | 2021-03-11 19:35:02,497 [WrapperSimpleAppMain] INFO  b.o.e.AuthorizationServiceMain - Started AuthorizationServiceMain in 22.659 seconds (JVM running for 24.385)
```

## First User and Application
Now that Ego is up and running we want to configure the first user and application that can use Ego for Authorization.

### Identify Provider

