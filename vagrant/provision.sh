#!/bin/bash

curl -sL https://deb.nodesource.com/setup_iojs_2.x | bash -
apt-get -y update
apt-get install -y iojs make

cd /vagrant

\cp vagrant/website.service /etc/systemd/system/website.service

make setup

systemctl enable website.service
systemctl start website.service
