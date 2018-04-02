#!/bin/bash

# check whether vault is setup or not
if [ -z "$EGO_VAULT_URI" ]
then
    exec $EGO_INSTALL_PATH/install/exec/start-server.sh
else
    if [ -z "$VAULT_TOKEN" ]
    then
        echo "Running with IAM role" $EGO_IAM_ROLE
        exec $EGO_INSTALL_PATH/install/exec/start-server-iam.sh
    else
        echo "Running with token"
        exec $EGO_INSTALL_PATH/install/exec/start-server-token.sh
    fi
fi
