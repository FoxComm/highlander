#!/bin/bash

# Strict mode
set -euo pipefail

# Reset default password
echo "Resetting Neo4j default password..."
curl -sSL \
    -H "Content-Type: application/json" \
    -XPOST -d "{\"password\":\"password\"}" \
    -u neo4j:neo4j \
    http://neo4j.service.consul:7474/user/neo4j/password

# Run endless loop
echo "Running endless loop..."
while [ 1 ]
do
  sleep 60 &
  wait $!
done
