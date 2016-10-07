#!/bin/bash

cd /usr/share

/usr/bin/java -Denv={{greenriver_env}} -D{{greenriver_env}}.kafka.broker={{kafka_server}} -D{{greenriver_env}}.elastic.host=elasticsearch://{{search_server}} -D{{greenriver_env}}.activity.phoenix.url=http://{{phoenix_server}}/v1 -D{{greenriver_env}}.avro.schemaRegistryUrl=http://{{schema_server}} -D{{greenriver_env}}.elastic.setup=true -jar {{greenriver_jar}}
