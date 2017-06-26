#!/usr/bin/env bash
set -euo pipefail

sed -i -- "s/host.name=.*$/host.name=$(hostname -i)/g" /etc/kafka/server.properties
/usr/bin/kafka-server-start /etc/kafka/server.properties
