#!/bin/bash

alias stop='systemctl stop'
alias start='systemct start'

#install node and npm
apt-get update 
apt-get install -y nodejs-legacy npm build-essential lua-cjson lua-cjson-dev

#setup vagrant user
sudo -u vagrant /vagrant/ops/vagrant/setup_vagrant_user.sh

cd /vagrant

#install nginx
apt-get install -y nginx-extras

#setup custom nginx config
cp ops/nginx/uuid4.lua /etc/nginx/uuid4.lua
cp ops/nginx/default /etc/nginx/sites-available/default
cp ops/nginx/nginx.conf /etc/nginx/nginx.conf

systemctl restart nginx

#install ashes as a service
cp ops/vagrant/ashes.service /etc/systemd/system/
systemctl restart ashes




