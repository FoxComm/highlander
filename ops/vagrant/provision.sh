#!/bin/bash

alias stop='systemctl stop'
alias start='systemctl start'

# Setup the VM
apt-get update
apt-get install -y build-essential

# Install the latest version of node.js through n
apt-get install -y nodejs-legacy npm
npm install -g n
n latest

# Install nginx
apt-get install -y lua-cjson nginx-extras

# Setup custom nginx config
cd /vagrant
cp ops/nginx/uuid4.lua /etc/nginx/uuid4.lua
cp ops/nginx/default /etc/nginx/sites-available/default
cp ops/nginx/nginx.conf /etc/nginx/nginx.conf

systemctl restart nginx

# Install ashes as a service
cp ops/vagrant/ashes.service /etc/systemd/system/
systemctl restart ashes
