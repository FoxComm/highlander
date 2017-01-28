#!/usr/bin/env bash

/usr/local/bin/bottledwater \
        --allow-unkeyed \
        --slot {{item.slot}} \
        --postgres {{item.db_connection_string}} \
        --broker {{consul_services.kafka}} \
        --schema-registry "http://{{consul_services.schema_registry}}"
