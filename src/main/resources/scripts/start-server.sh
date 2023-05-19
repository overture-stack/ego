#!/bin/bash
$EGO_INSTALL_PATH/install/bin/ego start \
    wrapper.app.parameter.4=--spring.profiles.active=$EGO_ACTIVE_PROFILES,jks \
    wrapper.app.parameter.5=--token.key-store=$EGO_KEYSTORE_PATH \
    set.SPRING_DATASOURCE_URL=jdbc:postgresql://$EGO_DB_HOST:$EGO_DB_PORT/$EGO_DB \
    set.SPRING_DATASOURCE_USERNAME=$EGO_DB_USER \
    set.SPRING_DATASOURCE_PASSWORD=$EGO_DB_PASS \
    set.SERVER_PORT=$EGO_SERVER_PORT \
    set.GOOGLE_CLIENT_IDS=$EGO_SERVER_GOOGLE_CLIENT_IDS \
    set.FACEBOOK_CLIENT_ID=$EGO_SERVER_FACEBOOK_APP_ID \
    set.FACEBOOK_CLIENT_SECRET=$EGO_SERVER_FACEBOOK_SECRET
