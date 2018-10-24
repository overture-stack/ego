#!/bin/bash

### =================================================== ###
#     Reference file for server environment variables     #
### =================================================== ###

# === Install Path
export EGO_INSTALL_PATH=
export EGO_KEYSTORE_PATH=

# === DB Config
export EGO_DB=
export EGO_DB_HOST=
export EGO_DB_PORT=

# Leave DB_USER AND DB_PASS empty if using VAULT
export EGO_DB_USER=
export EGO_DB_PASS=

# === App Server Config
export EGO_ACTIVE_PROFILES="default"
export EGO_SERVER_PORT=8081

# Leave IDs and Secrets empty if using VAULT
export EGO_SERVER_GOOGLE_CLIENT_IDS=""
export EGO_SERVER_FACEBOOK_APP_ID=""
export EGO_SERVER_FACEBOOK_SECRET=""

# === VAULT CONFIG
# Leave all below empty if not using VAULT
export VAULT_APPLICATION_NAME="development/oicr/ego"
export EGO_VAULT_URI=
export EGO_VAULT_SCHEME=
export EGO_VAULT_HOST=
export EGO_VAULT_PORT=
#leave IAM Role blank if using Token authentication
export EGO_IAM_ROLE=
#leave Token blank if using IAM Role
export VAULT_TOKEN=
