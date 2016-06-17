#!/bin/bash

/usr/local/bin/consul-alerts start --watch-checks --consul-addr=${CONSUL_SERVER} --consul-dc={CONSUL_DC} --consul-acl-token=""
