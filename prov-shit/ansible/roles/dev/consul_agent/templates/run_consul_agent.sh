#!/bin/bash

source /etc/consul.d/env
/usr/local/bin/consul agent -dev -ui-dir=/usr/local/share/consul -config-dir="/etc/consul.d" -dc dev -bind {{appliance_hostname}} -client 0.0.0.0
