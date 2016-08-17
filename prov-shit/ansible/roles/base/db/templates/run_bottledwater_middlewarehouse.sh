#!/bin/bash

/usr/local/bin/bottledwater \
        --allow-unkeyed \
        --slot {{bottledwater_slot_middlewarehouse}} \
        --postgres postgres://{{middlewarehouse_db_user}}@{{db_host}}/{{middlewarehouse_db_name}} \
        --broker {{kafka_server}} \
        --schema-registry "http://{{schema_server}}"
