#!/bin/bash

if [[ ! -d /usr/local/share/node ]]; then
    echo "Installing node"
    wget -q http://nodejs.org/dist/v0.12.4/node-v0.12.4-linux-x64.tar.gz
    tar -zxvf node-v0.12.4-linux-x64.tar.gz
    mv node-v0.12.4-linux-x64 /usr/local/share/node
    echo "PATH=\$PATH:/usr/local/share/node/bin" >> /etc/profile
fi

#install iojs
if [[ ! -d /usr/local/share/iojs ]]; then
    echo "Installing iojs"
    wget -q https://iojs.org/dist/v2.2.1/iojs-v2.2.1-linux-x64.tar.xz
    tar -xf iojs-v2.2.1-linux-x64.tar.xz
    mv iojs-v2.2.1-linux-x64 /usr/local/share/iojs
    echo "IOS_HOME=/usr/local/share/iojs" >> /etc/profile
    echo "PATH=\$IOS_HOME/bin:\$PATH" >> /etc/profile
fi

source /etc/profile

cd /vagrant
npm install
