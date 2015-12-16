#!/bin/bash

USER=`whoami`
export HOME="/home/$USER"


# install nvm
curl -o- https://raw.githubusercontent.com/creationix/nvm/v0.29.0/install.sh | bash
. ~/.nvm/nvm.sh
nvm install 4.2.1

echo "export PHOENIX_URL=http://10.240.0.3:9090" >> .bashrc

npm install gulp -g
npm install gulp




