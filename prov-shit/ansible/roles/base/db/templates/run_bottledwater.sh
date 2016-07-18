#!/bin/bash

/usr/local/bin/bottledwater -u --postgres=postgres://phoenix@{{db_host}}/phoenix_development --allow-unkeyed --broker={{kafka_server}} --schema-registry="http://{{schema_server}}"
