#!/bin/bash

. /etc/consul.d/env
/usr/local/bin/consul-alerts start --watch-checks --consul-addr=localhost:8500 --consul-dc=${consul_dc} --consul-acl-token=""
