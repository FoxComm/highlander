# Install

How to install "Green River" project locally.

Navigation:

* [Requirements](#requirements)
* [Building](#building)
* [Configuration](#configuration)
* [Running](#running)
* [Testing](#testing)
* [Troubleshooting](#troubleshooting)

## Requirements

* PostgresSQL 9.4.x
* Elasticsearch 1.7.x
* Kafka 0.9.x with Zookeeper - see quick installation [guide](https://github.com/FoxComm/FoxComm/blob/kafka/kafka/README.md).
* [Schema Registry](https://github.com/confluentinc/schema-registry)
* [Bottled Water](https://github.com/FoxComm/bottledwater-pg)

## Building

See [Building from source](https://github.com/FoxComm/bottledwater-pg#building-from-source).

If you have troubles with building `avro-c` dependency for bottledwater extension, just follow the commands in [Dockerfile](https://github.com/FoxComm/bottledwater-pg/blob/master/build/Dockerfile.build#L21-L39) directly.

## Configuration

* Elasticsearch - specify `cluster.name` in elasticsearch config.
* Postgres - see [configuration](https://github.com/FoxComm/bottledwater-pg#configuration) section.
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
		$ su kafka && ~/bin/kafka-consumer-groups.sh --list --zookeeper=localhost:2181
		```

## Running

1. Run Elasticsearch, Kafka and Schema Registry:

	```
	$ service elastisearch start
	$ su kafka && ~/bin/kafka-server-start.sh ./config/server.properties
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

## Testing

1. Update some data:

	```plsql
	update customers set name = 'Hijacked by Pavel' where id = 1;
	```

2. [Refresh](https://github.com/FoxComm/green-river/issues/5) related materialized views:

	```plsql
	refresh materialized view concurrently customers_ranking;
	refresh materialized view concurrently customer_orders_view;
	refresh materialized view concurrently customer_purchased_items_view;
	refresh materialized view concurrently customer_shipping_addresses_view;
	refresh materialized view concurrently customer_billing_addresses_view;
	refresh materialized view concurrently customer_store_credit_view;
	refresh materialized view concurrently customer_save_for_later_view;

	refresh materialized view concurrently customers_search_view;
	```

3. Check data in Elasticsearch:

	```
	$ curl -XGET 'http://localhost:9200/phoenix/customers_search_view/_search'
	```

## Troubleshooting

Drop replication slot in `psql phoenix_development` if bottledwater can't start properly:

```plsql
select pg_drop_replication_slot('bottledwater');
```

Drop extension:
	
```plsql
drop extension bottledwater;
```

Wipe all queue metadata:

	$ sudo service zookeeper stop
	$ sudo rm -rf /tmp/*
	$ sudo rm -rf /var/lib/zookeeper/*
	$ sudo service zookeeper start