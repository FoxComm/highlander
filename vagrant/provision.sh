#!/bin/bash

#install scala
if [[ ! -d /usr/local/share/scala ]]; then
    echo "downloading scala 2.11.6..."
    wget -q http://downloads.typesafe.com/scala/2.11.6/scala-2.11.6.tgz
    echo "installing scala..."
    tar -zxf scala-2.11.6.tgz
    mv scala-2.11.6 /usr/local/share/scala
    echo "SCALA_HOME=/usr/local/share/scala" >> /etc/profile
    echo "PATH=\$PATH:\$SCALA_HOME/bin" >> /etc/profile
fi


#install sbt
if [[ ! -f /etc/apt/sources.list.d/sbt.list ]]; then
    echo "deb http://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
fi
echo "installing a bunch of other stuff.."
apt-get update -y
apt-get install -y --force-yes sbt tmux openjdk-8-jdk postgresql unzip


#install flyway
if [[ ! -d /usr/local/share/flyway ]]; then
    echo "downloading flyway 3.2.1.."
    wget -q http://repo1.maven.org/maven2/org/flywaydb/flyway-commandline/3.2.1/flyway-commandline-3.2.1.zip
    echo "installing flyway.."
    unzip flyway-commandline-3.2.1.zip
    mv flyway-3.2.1 /usr/local/share/flyway
    chown -R vagrant:vagrant /usr/local/share/flyway 
    chmod g+x /usr/local/share/flyway
    echo "PATH=\$PATH:/usr/local/share/flyway/" >> /etc/profile
fi

#setup postgresql
#trust connections from localhost
sed -i 's/127.0.0.1\/32\s*md5/127.0.0.1\/32 trust/' /etc/postgresql/9.4/main/pg_hba.conf
sed -i 's/::1\/128\s*md5/::1\/128 trust/' /etc/postgresql/9.4/main/pg_hba.conf
sudo -u postgres createuser -s vagrant || {
    echo "postgres: vagrant user already created, ignoring"
}

systemctl restart postgresql

cd /vagrant
make configure
