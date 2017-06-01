#!/usr/bin/env bash

set -ue

# Reset default password
echo "Resetting Neo4j default password..."
curl -H "Content-Type: application/json" -X POST -d "{\"password\":\"password\"}" \
  -u neo4j:neo4j \
  http://neo4j.service.consul:7474/user/neo4j/password

# Run endless loop
echo "Running endless loop..."
while true
do
  # loop infinitely
done
