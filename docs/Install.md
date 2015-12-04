# Install

How to install "Green River" project locally.

## Requirements

* PostgresSQL 9.4.x
* Elasticsearch 1.7.x
* Zookeeper 3.x.x
* Kafka 0.9.x
* [Schema Registry](https://github.com/confluentinc/schema-registry)
* [Bottled Water](https://github.com/FoxComm/bottledwater-pg)

## Building

See [Building from source](https://github.com/FoxComm/bottledwater-pg#building-from-source).

If you have troubles with building `avro-c` dependency for bottledwater extension, just follow the commands in [Dockerfile](https://github.com/FoxComm/bottledwater-pg/blob/master/build/Dockerfile.build#L21-L39) directly.

## Configuration

* Elasticsearch - specify `cluster.name` in elasticsearch config
* Postgres - see [configuration](https://github.com/FoxComm/bottledwater-pg#configuration)
* Schema Registry - prepare basic configuration file:

	```
	port=8081
	kafkastore.connection.url=localhost:2181
	kafkastore.topic=_schemas
	debug=false
	```

* Green River - see `src/main/resources/application.conf`

	* Note: `elastic4s` library uses port 9300 by default, so you shouldn't probably change it.
	
	* Note: you can get proper Kafka consumer `groupId` via command-line tool: 

		```
		$ $KAFKA_HOME/bin/kafka-consumer-groups.sh --list --zookeeper=localhost:2181
		```

## Running

1. Run Elasticsearch, Kafka and Schema Registry:

	```
	$ service elastisearch start
	$ $KAFKA_HOME/bin/kafka-server-start.sh ./config/server.properties
	$ schema-registry-start ~/schema-registry.properties
	```

2. Run bottledwater to listen `phoenix_development` schema updates:

	```
	$ ./kafka/bottledwater --postgres=postgres://localhost/phoenix_development --allow-unkeyed
	```

3. Run consumer with specified environment:

	```
	$ sbt -Denv=localhost consume
	```

4. Check data in Elasticsearch:

	```
	$ curl -XGET 'http://localhost:9200/phoenix/regions/_search'
	```

## Troubleshooting

Drop replication slot in `psql` (don't forget to specify a schema via `\c`) if bottledwater won't start properly:

	$ select pg_drop_replication_slot('bottledwater');
