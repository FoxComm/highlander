#!/usr/bin/env bash

set -euo pipefail
IFS=$'\n\t'

curl -H "Content-Type: application/json" -X POST -d "{\"password\":\"{{neo4j_pass}}\"}" -u {{neo4j_user}}:{{neo4j_user}} http://{{neo4j_host}}:{{neo4j_http_port}}/user/{{neo4j_user}}/password