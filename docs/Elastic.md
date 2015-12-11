# Elastic

How to install "Green River" project locally (light version).

## Steps.

1. Install and run Elasticsearch 1.7.x, `cd` to current file directory.

2. Create an index and close it to apply dynamic settings:

	```
	$ curl -XPUT 'http://localhost:9200/phoenix'
	$ curl -XPOST 'http://localhost:9200/phoenix/_close'
	```

3. Apply dynamic settings (ngram filter configuration):

	```
	$ curl -XPUT 'localhost:9200/phoenix/_settings' -d @json/index_settings.json --header "Content-Type: application/json"
	```

4. Re-open index:

	```
	$ curl -XPOST 'http://localhost:9200/phoenix/_open'
	```

5. Create type mappings for orders and customers:

	```
	$ curl -XPUT 'http://localhost:9200/phoenix/customers_search_view/_mapping' -d @json/customers_mapping.json --header "Content-Type: application/json"
	$ curl -XPUT 'http://localhost:9200/phoenix/orders_search_view/_mapping' -d @json/orders_mapping.json --header "Content-Type: application/json"
	```

6. Install [elasticdump](https://github.com/taskrabbit/elasticsearch-dump) tool to index test dataset (4 customers and 5 orders):

	```
	$ elasticdump --bulk=true --input=json/data.json --output=http://localhost:9200
	```

7. See [Query.md](https://github.com/FoxComm/green-river/blob/master/docs/Query.md) for querying examples!
