We use [BuildKite](https://buildkite.com/) as our continuous integration server. The service provides a hosted interface for managing builds and build steps, but we provide the boxes (or agents, in BuildKite speak) that the tests run against.  

## 1. Provision a Machine  

Setup a new linux server (we use Ubuntu Server 14.04). This can be done through our Google Compute Engine account, or it can be done on a local box.  

Once installation is complete, create a user named _ci_ that has sudo access. The rest of the setup steps should be taken as that user.  

## 2. Update the Machine  

```
$ sudo apt-get update
$ sudo apt-get upgrade
```

## 3. Install Project Dependencies  

### Ubuntu build tools  

```
$ sudo apt-get build-essential
```

### PostgreSQL  

* Install Postgres  

    ```
    $ sudo apt-get install postgresql libpq-dev
    ```

* Create the `fox` user  

    ```
    $ sudo su - postgres  
    $ psql  
    postgres=# CREATE ROLE fox SUPERUSER LOGIN;  
    postgres=# \q  
    $ exit  
    $ sudo service postgresql restart  
    ```

* Create the CI database  

    ```
    $ createdb phoenix_ci
    ```

### Go  

_Note: don't use the version of Go that is in the Ubuntu repositories, as it is only at version 1.2. We use version 1.4._  

* Download Go  

    ```
    $ wget https://storage.googleapis.com/golang/go1.4.2.linux-amd64.tar.gz
    ```

* Install Go  

    ```
    $ sudo tar -C /usr/local -xzf go1.4.2.linux-amd64.tar.gz
    $ echo "export GOPATH=/home/ci/go" >> $HOME/.bashrc
    $ echo "export PATH=$PATH:/usr/local/go/bin" >> $HOME/.bashrc
    ```
    
### io.js

```
curl -sL https://deb.nodesource.com/setup_iojs_2.x | sudo bash -
sudo apt-get install -y iojs
```

### Install the BuildKite Agent  

* Install the Agent  
  * Follow the instructions on the BuildKite wiki: [https://buildkite.com/organizations/foxcommerce/agents/connect](https://buildkite.com/organizations/foxcommerce/agents/connect)  

* Customize the user that BuildKite runs under 
  * Open up `/etc/buildkite-agent.env` in your favorite editor  
  * Set `BUILDKITE_USER` and `BUILDKITE_USER_GROUP` to the value "fox"  

* Set custom environment variables that are needed for Go  

   ```
   $ cd /etc/buildkite-agent/hooks  
   $ sudo echo "#!/bin/bash" > environment
   $ sudo echo "set -e" >> environment
   $ sudo echo "export GOPATH=/home/ci/go" >> environment
   $ sudo echo "export PATH=$PATH:/usr/local/go/bin:$GOPATH/bin" >> environment
   ```  

* Restart BuildKite  

    ```
    $ sudo service buildkite-agent restart  
    ```  

* Setup the GOPATH  

   ```
   $ mkdir -p $HOME/go/src/github.com/FoxComm  
   $ cd $HOME/go/src/github.com/FoxComm
   $ ln -s /var/buildkite-agent/builds/<machine-name>/foxcommerce/phoenix phoenix  
   ```  

## 4. Profit!!  

You're done. The agent should show up in BuildKite and be available for use.
