#!/bin/bash

#source nvm
. /home/vagrant/.nvm/nvm.sh

#use correct version of node
nvm use 4.2.1

#start
cd /vagrant
npm start
