#!/bin/bash

source /etc/consul.d/env
/usr/local/bin/consul agent -server -ui-dir=/usr/local/share/consul -config-dir="/etc/consul.d" -${CONSUL_JOIN_TYPE}=${CONSUL_SERVER} -dc=${CONSUL_DC} -bind=${CONSUL_BIND}
