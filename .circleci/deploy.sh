#!/bin/bash

### =================================================== ###
#                                                         #
# CircleCI Deploy Script                                  #
#                                                         #
#                                                         #
# Ensure that a valid ssh key has been added to Circle CI #
#                                                         #
# Set the following environment variables in CircleCI to  #
#   configure this deploy script:                         #
#                                                         #
#  - EGO_DEPLOY_USER                                      #
#  - EGO_DEPLOY_SERVER                                    #
#  - EGO_INSTALL_PATH                                     #
#  - EGO_ENV_FILE_PATH                                    #
#                                                         #
# Also before running, on the deploy server:              #
#   - configure all env variables listed in               #
#      /env_template.sh so that the start & stop scripts  #
#      will run.                                          #
#   - create an install path owned by the EGO_DEPLOY_USER #
#   - create a `builds` directory inside the install path #
#                                                         #
### =================================================== ###

# === Sync Resource Files
rsync -azP src/main/resources "$EGO_DEPLOY_USER@$EGO_DEPLOY_SERVER:$EGO_INSTALL_PATH"

# === Find generated filename
for i in target/ego-*.tar.gz; do
    filepath=$i;
done
echo "filepath: $filepath"

file=$(basename $filepath)
echo "file: $file"

# === Upload New Uber-Jar
scp $filepath "$EGO_DEPLOY_USER@$EGO_DEPLOY_SERVER:$EGO_INSTALL_PATH/builds/"

# === Unpack Jar
ssh "$EGO_DEPLOY_USER@$EGO_DEPLOY_SERVER" "tar zxvf $EGO_INSTALL_PATH/builds/$file -C $EGO_INSTALL_PATH/builds"

# === Stop Existing Service
ssh "$EGO_DEPLOY_USER@$EGO_DEPLOY_SERVER" "source $EGO_ENV_FILE_PATH; $EGO_INSTALL_PATH/resources/scripts/stop-server.sh"

# === Update Symlink
extract_folder="${file%-dist.tar.gz}"
ssh "$EGO_DEPLOY_USER@$EGO_DEPLOY_SERVER" "ln -sf $EGO_INSTALL_PATH/builds/$extract_folder $EGO_INSTALL_PATH/install"

# === Start Existing Service
ssh "$EGO_DEPLOY_USER@$EGO_DEPLOY_SERVER" "source $EGO_ENV_FILE_PATH; $EGO_INSTALL_PATH/resources/scripts/start-server.sh"