#!/bin/bash
# === Add token profile
export EGO_TOKEN_PROFILE=$EGO_ACTIVE_PROFILES,app,db,token

# === Start Server
$EGO_INSTALL_PATH/install/bin/ego start \
    wrapper.app.parameter.4=--spring.profiles.active=EGO_TOKEN_PROFILE \
    wrapper.app.parameter.5=--token.key-store=$EGO_KEYSTORE_PATH \
    set.SPRING_DATASOURCE_URL=jdbc:postgresql://$EGO_DB_HOST:$EGO_DB_PORT/$EGO_DB \
    set.SERVER_PORT=$EGO_SERVER_PORT \
    set.SPRING_APPLICATION_NAME=$VAULT_APPLICATION_NAME \
    set.SPRING_CLOUD_VAULT_URI=$EGO_VAULT_URI \
    set.SPRING_CLOUD_VAULT_SCHEME=$EGO_VAULT_SCHEME \
    set.SPRING_CLOUD_VAULT_HOST=$EGO_VAULT_HOST \
    set.SPRING_CLOUD_VAULT_PORT=$EGO_VAULT_PORT \
    set.SPRING_CLOUD_VAULT_TOKEN=$VAULT_TOKEN
