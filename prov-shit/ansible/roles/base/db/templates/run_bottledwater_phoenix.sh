#!/bin/bash

/usr/local/bin/bottledwater \
        --allow-unkeyed \
        --slot {{bottledwater_slot_phoenix}} \
        --postgres=postgres://phoenix@{{db_host}}/{{db_name}} \
        --broker={{kafka_server}} \
        --schema-registry="http://{{schema_server}}"
