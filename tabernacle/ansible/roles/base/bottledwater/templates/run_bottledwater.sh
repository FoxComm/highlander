#!/usr/bin/env bash
set -euo pipefail

/usr/local/bin/bottledwater \
        --allow-unkeyed \
        --slot {{item.slot}} \
        --postgres {{item.db_connection_string}} \
        --broker {{kafka_server}} \
        --schema-registry "http://{{schema_server}}"
