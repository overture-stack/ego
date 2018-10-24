#!/bin/bash

# === Add IAM profile
export EGO_IAM_PROFILE=$EGO_ACTIVE_PROFILES,db,app,iam

# === Add token profile
export EGO_TOKEN_PROFILE=$EGO_ACTIVE_PROFILES,db,app,token


# check whether vault is setup or not
if [ -z "$EGO_VAULT_URI" ]
then
    java -jar $EGO_INSTALL_PATH/install/ego.jar \
        --spring.profiles.active=$EGO_ACTIVE_PROFILES \
        --spring.datasource.url="jdbc:postgresql://$EGO_DB_HOST:$EGO_DB_PORT/$EGO_DB?stringtype=unspecified" \
        --spring.datasource.username="$EGO_DB_USER" \
        --spring.datasource.password="$EGO_DB_PASS" \
        --google.client.Ids="$EGO_SERVER_GOOGLE_CLIENT_IDS" \
        --facebook.client.id="$EGO_SERVER_FACEBOOK_APP_ID" \
        --facebook.client.secret="$EGO_SERVER_FACEBOOK_SECRET" \
        --server.port=$EGO_SERVER_PORT
else
    if [ -z "$VAULT_TOKEN" ]
    then
        echo "Running with IAM role" $EGO_IAM_ROLE
        java -jar $EGO_INSTALL_PATH/install/ego.jar \
            --spring.profiles.active=$EGO_IAM_PROFILE \
            --spring.datasource.url="jdbc:postgresql://$EGO_DB_HOST:$EGO_DB_PORT/$EGO_DB?stringtype=unspecified" \
            --server.port=$EGO_SERVER_PORT \
            --spring.application.name=$EGO_VAULT_APPLICATION_NAME \
            --spring.cloud.vault.uri=$EGO_VAULT_URI \
            --spring.cloud.vault.scheme=$EGO_VAULT_SCHEME \
            --spring.cloud.vault.host=$EGO_VAULT_HOST \
            --spring.cloud.vault.port=$EGO_VAULT_PORT \
            --spring.cloud.vault.aws-iam.role=$EGO_IAM_ROLE
    else
        echo "Running with Vault token"
        java -jar $EGO_INSTALL_PATH/install/ego.jar \
            --spring.profiles.active=$EGO_TOKEN_PROFILE \
            --spring.datasource.url="jdbc:postgresql://$EGO_DB_HOST:$EGO_DB_PORT/$EGO_DB?stringtype=unspecified" \
            --server.port=$EGO_SERVER_PORT \
            --spring.application.name=$EGO_VAULT_APPLICATION_NAME \
            --spring.cloud.vault.uri=$EGO_VAULT_URI \
            --spring.cloud.vault.scheme=$EGO_VAULT_SCHEME \
            --spring.cloud.vault.host=$EGO_VAULT_HOST \
            --spring.cloud.vault.port=$EGO_VAULT_PORT \
            --spring.cloud.vault.token=$VAULT_TOKEN
    fi
fi
