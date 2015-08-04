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
    sbt seeds
    sbt '~re-start'
    ```

    The server will now be accessible on port 9090 on the host.

### Running Locally

#### Dependencies on OSX

- [JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

    http://computechtips.com/781/install-oracle-jdk-8-mac-os-x-10-10-yosemite

- [SBT](http://www.scala-sbt.org/)

    ```bash
    brew install sbt
    ```

- [Scala](http://www.scala-lang.org/)

    ```bash
    brew install scala
    ```

- [Flyway](http://flywaydb.org/getstarted/)

    ```bash
    brew install flyway
    ```

- PostgreSQL 

    ```bash
    brew install postgresql
    ```

### Dependencies on Ubuntu

We use Ubuntu 14.04 (Trusty) as the base of our Vagrant image. For details about how to set this up on local Ubuntu, check out the provisioning script: [provision.sh](https://github.com/FoxComm/phoenix-scala/blob/master/vagrant/provision.sh).

_Note:_ OpenJDK has been known to cause issues in Ubuntu with Scala. Make sure to use the Oracle JDK.

### Setup

1. Setup the database

    ```bash
    make configure
    ```
2. Run SBT and verify that the application compiles

    ```bash
    sbt compile
    ```

3. If you plan on running the local development server, execute the seeds and starte the server

    ```bash
    sbt seeds
    sbt '~re-start'
    ```

### Useful Commands  

- `make resetdb`: resets the database (drops and reruns migrations)
- `sbt '~re-start'`: reloads the application automatically on code changes
- `sbt seeds`: execute the seeds
- `sbt test`: run all of the unit and integration tests
- `sbt '~test:compile`: re-compiles the application automatically on code changes

