#!/bin/bash

set -ue

DIR=ansible/roles/base/secret_keys/files/secret_keys/$1
if [[ ! -d $DIR ]]; then
  mkdir -p $DIR
fi

openssl genrsa -out $DIR/private_key.pem 4096
openssl pkcs8 -topk8 -inform PEM -outform DER -in $DIR/private_key.pem -out $DIR/private_key.der -nocrypt
openssl rsa -in $DIR/private_key.pem -pubout -outform DER -out $DIR/public_key.der
openssl rsa -in $DIR/private_key.pem -pubout -outform PEM -out $DIR/public_key.pem

ansible-vault encrypt $DIR/private_key.pem
ansible-vault encrypt $DIR/private_key.der
