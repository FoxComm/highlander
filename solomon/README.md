# Solomon

These are the basic steps that you should take to get the application running:

  * Install dependencies with `mix deps.get`
  * This application shares a database with phoenix.  *Be sure to run phoenix migrations*: ``flyway migrate -X -locations=filesystem:`pwd` ``
  * Start Phoenix endpoint with `source .env && mix phoenix.server`

Now you can visit [`localhost:4002`](http://localhost:4002) from your browser.

## JWT signing and verifying

The application looks for the following environment variables

  * `PRIVATE_KEY` : the full name of the private key
  * `PUBLIC_KEY` : the full name of the public key
  * `TOKEN_TTL` : a default number of days before a token should expire

You can set these variables in `.env.local` before running `source .env`.

## How-to generate RSA keys:

```bash
openssl genrsa -out private_key.pem 4096
openssl rsa -in private_key.pem -pubout -outform PEM -out public_key.pem
```
