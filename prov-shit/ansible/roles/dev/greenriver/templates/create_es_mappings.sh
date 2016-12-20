#!/bin/bash

docker pull {{docker_registry}}:5000/greenriver:{{docker_frontend_branch}}
docker run --network host -t {{docker_registry}}:5000/greenriver:{{docker_frontend_branch}} java -Denv={{greenriver_env}} -D{{greenriver_env}}.kafka.broker={{kafka_server}} -D{{greenriver_env}}.elastic.host=elasticsearch://{{search_server}} -D{{greenriver_env}}.activity.phoenix.url=http://{{phoenix_server}}/v1 -D{{greenriver_env}}.avro.schemaRegistryUrl=http://{{schema_server}} -D{{greenriver_env}}.elastic.setup=true -jar /green-river/green-river-assembly-1.0.jar
