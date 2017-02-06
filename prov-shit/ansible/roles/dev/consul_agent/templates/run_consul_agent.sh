#!/usr/bin/env bash

source /etc/consul.d/env
HOST={{mesos_ip}}

/usr/local/bin/consul agent -dev \
    -ui-dir=/usr/local/share/consul \
    -config-dir="/etc/consul.d" \
    -datacenter=dev \
    -bind=$HOST \
    -client 0.0.0.0
