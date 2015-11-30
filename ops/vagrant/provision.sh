#!/bin/bash

alias stop='systemctl stop'
alias start='systemct start'

#install node and npm
apt-get udpate 
apt-get install -y nodejs-legacy npm build-essential lua-cjson

#setup vagrant user
sudo -u vagrant /vagrant/ops/vagrant/setup_vagrant_user.sh

cd /vagrant

#install nginx
apt-get install -y nginx-extras
cp ops/nginx/default /etc/nginx/sites-available/default
systemctl restart nginx

#install ashes as a service
cp ops/vagrant/ashes.service /etc/systemd/system/
systemctl restart ashes




