#!/usr/bin/env bash
CONSUL_DC=$1
CONSUL_SERVER=$2
PRIVATE_IP=$(curl -sf http://169.254.169.254/latest/meta-data/local-ipv4 | tr -d '\n')

sudo bash -c "echo 'CONSUL_DC=$CONSUL_DC' >> /etc/consul.d/env"
sudo bash -c "echo 'CONSUL_SERVER=$CONSUL_SERVER' >> /etc/consul.d/env"
sudo bash -c "echo 'CONSUL_BIND=$PRIVATE_IP' >> /etc/consul.d/env"
