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
#   - create a file on the server that configures all     #
#      environment variables listed in /env_template.sh ; #
#      These values are needed for that the start & stop  #
#      scripts will run ; add the path to this file on    #
#      the server in the CircleCI environment variable:   #
#        EGO_ENV_FILE_PATH                                #
#   - create an install path owned by the EGO_DEPLOY_USER #
#   - create a `builds` directory inside the install path #
#                                                         #
### =================================================== ###


# === Sync Resource Files
echo "# ===== START rsync of resource files ===== #"
rsync -azP src/main/resources "$EGO_DEPLOY_USER@$EGO_DEPLOY_SERVER:$EGO_INSTALL_PATH"
echo "# ===== END rsync of resource files ===== #\n\n"


# === Upload Build
echo "# ===== START Upload build package ===== #"

# = Find generated filename
for i in target/ego-*.tar.gz; do
    filepath=$i;
done
echo "filepath: $filepath"

file=$(basename $filepath)
echo "file: $file"

# = scp file to server
echo "Upload Starting:"
scp $filepath "$EGO_DEPLOY_USER@$EGO_DEPLOY_SERVER:$EGO_INSTALL_PATH/builds/"

echo "# ===== END Upload build package ===== #\n\n"


# === Unpack Jar
echo "# ===== START Unpack jar ===== #"
ssh "$EGO_DEPLOY_USER@$EGO_DEPLOY_SERVER" "tar zxvf $EGO_INSTALL_PATH/builds/$file -C $EGO_INSTALL_PATH/builds"
echo "# ===== END Unpack jar ===== #\n\n"

# === Stop Existing Service
echo "# ===== START Stop server ===== #"
ssh "$EGO_DEPLOY_USER@$EGO_DEPLOY_SERVER" "source $EGO_ENV_FILE_PATH; $EGO_INSTALL_PATH/resources/scripts/stop-server.sh"
echo "# ===== END Stop server ===== #\n\n"

# === Update Symlink
echo "# ===== START Update Installed version link ===== #"
extract_folder="${file%-dist.tar.gz}"
ssh "$EGO_DEPLOY_USER@$EGO_DEPLOY_SERVER" "ln -sf $EGO_INSTALL_PATH/builds/$extract_folder $EGO_INSTALL_PATH/install"
echo "# ===== END Update Installed version link ===== #\n\n"

# === Start Existing Service
echo "# ===== START Start Server ===== #"
ssh "$EGO_DEPLOY_USER@$EGO_DEPLOY_SERVER" "source $EGO_ENV_FILE_PATH; $EGO_INSTALL_PATH/resources/scripts/start-server.sh"
echo "# ===== END Start Server ===== #"

echo "\n\n\n# ============== SUCCESS!"