#!/bin/bash

#source nvm
. /home/vagrant/.nvm/nvm.sh


#use correct version of node
cd /vagrant
nvm use  # uses .nvmrc
npm build
npm install
gulp server
