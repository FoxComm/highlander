#!/usr/bin/env bash

source /etc/consul.d/env
/usr/local/bin/consul agent -ui-dir=/usr/local/share/consul -config-dir="/etc/consul.d" -join ${CONSUL_SERVER} -dc ${CONSUL_DC} -bind ${CONSUL_BIND} -client 0.0.0.0
