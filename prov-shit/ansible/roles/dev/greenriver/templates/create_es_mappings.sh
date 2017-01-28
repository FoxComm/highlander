#!/usr/bin/env bash

docker pull {{docker_registry}}:5000/greenriver:{{docker_tags.greenriver}}
docker run --network host -t {{docker_registry}}:5000/greenriver:{{docker_tags.greenriver}} java -Denv={{greenriver_env}} -D{{greenriver_env}}.kafka.broker={{consul_services.kafka}} -D{{greenriver_env}}.elastic.host=elasticsearch://{{consul_services.es_tcp}} -D{{greenriver_env}}.activity.phoenix.url=http://{{consul_services.phoenix}}/v1 -D{{greenriver_env}}.avro.schemaRegistryUrl=http://{{consul_services.schema_registry}} -D{{greenriver_env}}.elastic.setup=true -jar /green-river/green-river-assembly-1.0.jar
