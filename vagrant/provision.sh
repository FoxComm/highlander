#!/bin/bash

set -x


#install scala
if [[ ! -d /usr/local/share/scala ]]; then
    echo "downloading scala 2.11.7..."
    wget -q http://downloads.typesafe.com/scala/2.11.7/scala-2.11.7.tgz
    echo "installing scala..."
    tar -zxf scala-2.11.7.tgz
    mv scala-2.11.7 /usr/local/share/scala
    echo "SCALA_HOME=/usr/local/share/scala" >> /etc/profile
    echo "PATH=\$PATH:\$SCALA_HOME/bin" >> /etc/profile
fi

apt-get install -y software-properties-common python-software-properties debconf-utils
add-apt-repository ppa:webupd8team/java -y
apt-get update
echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | sudo debconf-set-selections
apt-get install -y oracle-java8-installer
apt-get install -y oracle-java8-set-default

#install sbt
if [[ ! -f /etc/apt/sources.list.d/sbt.list ]]; then
    echo "deb http://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
fi

echo "installing a bunch of other stuff.."
apt-get update -y
apt-get install -y --force-yes sbt tmux unzip make

#install the schema-registry from confluent
wget -qO - http://packages.confluent.io/deb/1.0/archive.key | sudo apt-key add -
add-apt-repository "deb [arch=all] http://packages.confluent.io/deb/1.0 stable main"
apt-get update  -y 
apt-get install -y confluent-schema-registry 

#install kafka 
if [[ ! -d /home/vagrant/kafka ]]; then
    wget http://apache.cs.utah.edu/kafka/0.9.0.0/kafka_2.11-0.9.0.0.tgz
    tar -zxf kafka_2.11-0.9.0.0.tgz
    mv kafka_2.11-0.9.0.0 /home/vagrant/kafka

    #copy service files
    cp /vagrant/vagrant/kafka.service /etc/systemd/system/
    cp /vagrant/vagrant/zookeeper.service /etc/systemd/system/
    systemctl enable kafka
    systemctl enable zookeeper
    systemctl start kafka
    systemctl start zookeeper
fi

#install elasticsearch 
if [[ ! -d /home/vagrant/elasticsearch ]]; then
    wget https://download.elastic.co/elasticsearch/elasticsearch/elasticsearch-1.7.3.zip
    unzip elasticsearch-1.7.3.zip
    mv elasticsearch-1.7.3 /home/vagrant/elasticsearch
    cp /vagrant/vagrant/elasticsearch.service /etc/systemd/system/
    systemctl enable elasticsearch
    systemctl start elasticsearch
fi

