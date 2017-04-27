#!/usr/bin/env bash
CONSUL_DC=$1
CONSUL_SERVER=$2
PRIVATE_IP=$(curl -sf -H 'Metadata-Flavor:Google' http://metadata/computeMetadata/v1/instance/network-interfaces/0/ip | tr -d '\n')

sudo bash -c "echo 'CONSUL_DC=$CONSUL_DC' >> /etc/consul.d/env"
sudo bash -c "echo 'CONSUL_SERVER=$CONSUL_SERVER' >> /etc/consul.d/env"
sudo bash -c "echo 'CONSUL_BIND=$PRIVATE_IP' >> /etc/consul.d/env"
