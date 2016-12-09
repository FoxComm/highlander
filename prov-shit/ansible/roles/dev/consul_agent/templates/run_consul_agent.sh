#!/bin/bash

source /etc/consul.d/env
HOST={{appliance_hostname}}
/usr/local/bin/consul agent -dev -ui-dir=/usr/local/share/consul -config-dir="/etc/consul.d" -dc dev -bind $HOST -client 0.0.0.0
