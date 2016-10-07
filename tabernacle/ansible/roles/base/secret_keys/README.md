## How-to generate RSA keys:


```bash
openssl genrsa -out private_key.pem 4096
openssl pkcs8 -topk8 -inform PEM -outform DER -in private_key.pem -out private_key.der -nocrypt
openssl rsa -in private_key.pem -pubout -outform DER -out public_key.der
openssl rsa -in private_key.pem -pubout -outform PEM -out public_key.pem
```

`.der` used at phoenix

and `.pem` used at Ashes-server

## Decrypt

For encrypting/decrypting use `ansuble-vault` tool
