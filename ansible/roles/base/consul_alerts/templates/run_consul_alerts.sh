#!/bin/bash

/usr/local/bin/consul-alerts start --alert-addr=localhost:9000 --consul-addr=${CONSUL_SERVER} --consul-dc={CONSUL_DC} --consul-acl-token=""
