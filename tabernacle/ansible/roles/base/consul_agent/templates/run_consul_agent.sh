#!/usr/bin/env bash
set -euo pipefail

source /etc/consul.d/env

{% if is_appliance %}
HOST={{mesos_ip}}
/usr/local/bin/consul agent -dev \
    -ui-dir=/usr/local/share/consul \
    -config-dir="/etc/consul.d" \
    -dc=dev \
    -bind=$HOST \
    -client 0.0.0.0
{% else %}
/usr/local/bin/consul agent -ui-dir=/usr/local/share/consul \
    -config-dir="/etc/consul.d" \
    -join ${CONSUL_SERVER} \
    -dc ${CONSUL_DC} \
    -bind ${CONSUL_BIND} \
    -client 0.0.0.0
{% endif %}

