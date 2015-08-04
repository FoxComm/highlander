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

- Java (must be Oracle version of Java, not OpenJDK)

    ```bash
    sudo apt-get install -y software-properties-common python-software-properties debconf-utils
    sudo add-apt-repository ppa:webupd8team/java -y
    sudo apt-get update
    echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | sudo debconf-set-selections
    sudo apt-get install -y oracle-java8-installer
    sudo apt-get install -y oracle-java8-set-default
    ```

- SBT

    ```bash
    echo "deb http://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
    sudo apt-get update -y
    sudo apt-get install -y sbt
    ```

- Scala

    ```bash
    wget -q http://downloads.typesafe.com/scala/2.11.6/scala-2.11.6.tgz
    tar -zxf scala-2.11.6.tgz
    sudo mv scala-2.11.6 /usr/local/share/scala
    echo "SCALA_HOME=/usr/local/share/scala" >> $HOME/.bashrc
    echo "PATH=\$PATH:\$SCALA_HOME/bin" >> $HOME/.bashrc
    ```

- Flyway

    ```bash
    wget -q http://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/3.2.1/flyway-commandline-3.2.1.zip
    unzip flyway-commandline-3.2.1.zip
    sudo mv flyway-3.2.1 /usr/local/share/flyway
    sudo chown -R $USER /usr/local/share/flyway 
    sudo chmod g+x /usr/local/share/flyway
    echo "PATH=\$PATH:/usr/local/share/flyway/" >> $HOME/.bashrc
    ```

- PostgreSQL 9.4

    ```bash
    echo "deb http://apt.postgresql.org/pub/repos/apt/ trusty-pgdg main" | tee -a /etc/apt/sources.list.d/postgres.list
    wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add -
    sudo apt-get update
    sudo apt-get install -y postgresql-9.4
    ```

    In `pg_hba.conf`, make sure that all connections from 127.0.0.1 are set to trust, not peer or md5.

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

