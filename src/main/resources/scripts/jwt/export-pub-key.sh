keytool -list -rfc --keystore $1 | openssl x509 -inform pem -pubkey
