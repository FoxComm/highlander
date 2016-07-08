[![Build status](https://badge.buildkite.com/20bc913b3e06b49544cd4354c92f675bdfd0cf93f5a4640d3e.svg)](https://buildkite.com/foxcommerce/phoenix-scala)

# Phoenix

<p align="center">
  <img src="http://images2.alphacoders.com/451/451370.jpg">
</p>

## Development

Phoenix can be run in development by running either running the application natively on your computer or through Vagrant. Instructions for both are below:

### Using Vagrant

1. Provision a new guest instance in Vagrant

    ```bash
    vagrant up
    ```

2. SSH into the guest and navigate to the working directory

    ```bash
    vagrant ssh
    cd /vagrant
    ```

3. Run SBT and verify that the application compiles

    ```bash
    sbt compile
    ```

4. If you plan on running the local development server, execute the seeds and starte the server

    ```bash
    sbt seed
    sbt '~re-start'
    ```

    The server will now be accessible on port 9090 on the host.

### Running Locally

#### Dependencies on OSX

- [JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html), [SBT](http://www.scala-sbt.org/), [Scala](http://www.scala-lang.org/), [Flyway](http://flywaydb.org/getstarted/), PostgreSQL

    ```bash
    brew cask install java
    brew install sbt scala flyway postgresql
    ```

### Useful Commands  

- `sbt '~re-start'`: reloads the application automatically on code changes
- `sbt seed`: execute the seeds
- `sbt test`: run all of the unit and integration tests
- `sbt '~test:compile`: re-compiles the application automatically on code changes

### Auth

For proper auth and outh work. some `ENV` keys should be setuped:

#### Base auth

```
export PHOENIX_PUBLIC_KEY=/Users/narma/w/rsa/public_key.der
export PHOENIX_PRIVATE_KEY=/Users/narma/w/rsa/private_key.der
export PHOENIX_AUTH_METHOD=jwt
export PHOENIX_COOKIE_SECURE=off
```

RSA keys for staging and instruction how-to generate own keys located here:
https://github.com/FoxComm/prov-shit/blob/master/ansible/roles/secret_keys/README.md

#### Google oauth

For admin part:
```
export GOOGLE_OAUTH_ADMIN_CLIENT_ID=953682058057-cse9mkmr1ot9ps79o3qrut39kjrhd6da.apps.googleusercontent.com
export GOOGLE_OAUTH_ADMIN_CLIENT_SECRET=RVMs--sfNa3H2AcK2hDeazRc
export GOOGLE_OAUTH_ADMIN_REDIRECT_URI=http://localhost:1080/api/v1/public/oauth2callback/google/admin
```

For customer part:
```
export GOOGLE_OAUTH_CUSTOMER_CLIENT_ID=$GOOGLE_OAUTH_ADMIN_CLIENT_ID
export GOOGLE_OAUTH_CUSTOMER_CLIENT_SECRET=$GOOGLE_OAUTH_ADMIN_CLIENT_SECRET
export GOOGLE_OAUTH_CUSTOMER_REDIRECT_URI=http://localhost:1080/api/v1/public/oauth2callback/google/customer
```

These keys works only for `localhost` domain and ports `4000`, `1080`

### Image uploading to S3

To upload images to S3, you should set the following environment variables:

- `AWS_ACCESS_KEY`
- `AWS_SECRET_KEY`
- `S3_BUCKET`
- `S3_REGION`

These settings are in `prov-shit` in the location: `ansible/roles/dev/phoenix/vars/aws.yml`. You'll need to use `ansible-vault` to decrypt and get the settings.
