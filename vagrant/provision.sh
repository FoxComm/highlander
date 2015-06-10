#!/bin/bash

curl -sL https://deb.nodesource.com/setup_iojs_2.x | bash -
apt-get -y update
apt-get install -y iojs

cd /vagrant

make setup
