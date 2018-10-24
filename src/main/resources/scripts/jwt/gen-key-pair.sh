#!/bin/bash

# generate pubkey and private keys
openssl genrsa -out private_key.pem 2048
openssl rsa -in private_key.pem -pubout -out public_key.pem
openssl pkcs8 -topk8 -in private_key.pem -inform pem -out private_key_pkcs8.pem -outform pem -nocrypt
awk '{ printf "%s", $0 }' public_key.pem | awk '{ gsub("-----BEGIN PUBLIC KEY-----","",$0); print $0 }' | awk '{ gsub("-----END PUBLIC KEY-----","",$0); print $0 }' > public_key_text.pem
awk '{ printf "%s", $0 }' private_key_pkcs8.pem | awk '{ gsub("-----BEGIN PRIVATE KEY-----","",$0); print $0 }' | awk '{ gsub("-----END PRIVATE KEY-----","",$0); print $0 }' > private_key_text.pem

# cleanup
rm private_key.pem private_key_pkcs8.pem public_key.pem
