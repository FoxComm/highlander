#!/usr/bin/env bash

source /etc/consul.d/env
/usr/local/bin/consul-alerts start --watch-checks --consul-addr=localhost:8500 --consul-dc=$CONSUL_DC --consul-acl-token="" --log-level info
