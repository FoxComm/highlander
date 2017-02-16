#!/usr/bin/env bash

source /etc/consul.d/env
/usr/local/bin/consul agent -server -ui-dir=/usr/local/share/consul -config-dir="/etc/consul.d" -{{ join_type }}=${CONSUL_SERVER} -dc=${CONSUL_DC} -bind=${CONSUL_BIND}
