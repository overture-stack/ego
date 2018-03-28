#!/bin/bash

echo 'To test vault locally: download vault from: https://www.vaultproject.io/downloads.html and unzip in this folder'
echo 'start vault server using : ./vault server -config ./vault.conf'
echo 'Once server is running, execute this file again to setup required keys in vault'

# Vault server address
export VAULT_ADDR=http://localhost:8200

# initialize vault
export OUTPUT=$(./vault operator init)

# set token
export VAULT_TOKEN=$(echo $(echo $OUTPUT | awk -F'Token: ' '{print$2}' | awk -F' Vault' '{print $1}'))

echo 'User this token in bootstrap-token.properties:' $VAULT_TOKEN

# grab all unseal keys
export VAULT_UNSEAL_KEY1=$(echo $(echo $OUTPUT | awk -F'Unseal Key 1:' '{print$2}' | awk -F' Unseal' '{print $1}'))
export VAULT_UNSEAL_KEY2=$(echo $(echo $OUTPUT | awk -F'Unseal Key 2:' '{print$2}' | awk -F' Unseal' '{print $1}'))
export VAULT_UNSEAL_KEY3=$(echo $(echo $OUTPUT | awk -F'Unseal Key 3:' '{print$2}' | awk -F' Unseal' '{print $1}'))

# unseal vault
./vault operator unseal $VAULT_UNSEAL_KEY1
./vault operator unseal $VAULT_UNSEAL_KEY2
./vault operator unseal $VAULT_UNSEAL_KEY3

./vault write secret/development/oicr/ego/dev spring.datasource.username=postgres spring.datasource.password=postgres facebook.client.id=140524976574963 facebook.client.secret=2439abe7ae008bda7ab5cfdf706b4d66 google.client.Ids=808545688838-99s198l9lhl2hsvkpo5u91f3sflegemp.apps.googleusercontent.com,911372380614-7m296bg4eadc7m43e2mm6fs1a0ggkke1.apps.googleusercontent.com,814606937527-v7tr5dfqegjijicq3jeu5arv5tcl4ks0.apps.googleusercontent.com,814606937527-kk7ooglk6pj2tvpn7ldip6g3b74f8o72.apps.googleusercontent.com token.key-alias=ego-jwt token.keystore-password:=eG0tistic@lly
./vault read /secret/development/oicr/ego/dev
