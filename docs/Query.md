# Query

How to query `phoenix` index in Elasticsearch.

## Index structure

Currently, we store multiple types in Elasticsearch:
* `countries`
* `regions` 
* `customers_orders_view`

Each type has a list of allowed fields (others will be filtered by consumer) with different field types and indexing rules.

Most general examples are:
* `integer`, `long` - used for numeric comparsions, e.g. [Range Query](https://www.elastic.co/guide/en/elasticsearch/reference/1.7/query-dsl-range-query.html)
* `string`
  * `not_analyzed` - used for ADT comparsion, when we know predefined range of values (e.g. `status`, `currency`)
  * `autocomplete` - custom analyzer used for partial match, based on [Edge NGram Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/1.7/analysis-edgengram-tokenizer.html)
* `boolean` - simplified ADT with two possible values, usually filtered by [Term Filter](https://www.elastic.co/guide/en/elasticsearch/reference/1.7/query-dsl-term-query.html)

## Example

Query below returns a list of orders, that:
* Have `Yax` substring in his name

Since we're using [And Filter](https://www.elastic.co/guide/en/elasticsearch/reference/1.7/query-dsl-and-filter.html), all conditions must be satisfied.

### Request

URL: `http://localhost:9200/phoenix/customers_orders_view/_search`

```json
{
    "query": {
        "filtered": {
            "filter": {
                "and": [
                    {"bool": {"must": {"query": {"match": {"name": "Yax"}}}}}
                ]
            }    
        }
    },
    "from": 0,
    "size": 10
}
```

### Response

```json
{
   "took": 5,
   "timed_out": false,
   "_shards": {
      "total": 5,
      "successful": 5,
      "failed": 0
   },
   "hits": {
      "total": 1,
      "max_score": 1,
      "hits": [
         {
            "_index": "phoenix",
            "_type": "customers_orders_view",
            "_id": "1",
            "_score": 1,
            "_source": {
               "id": 1,
               "name": "Yax Fuentes",
               "email": "yax@yax.com",
               "is_blacklisted": false,
               "shipping_addresses": [
                  {
                     "address1": "555 E Lake Union St.",
                     "address2": null,
                     "city": "Seattle",
                     "zip": "12345",
                     "region_name": "Washington",
                     "country_name": "United States",
                     "country_continent": "North America",
                     "country_currency": "USD"
                  }
               ],
               "billing_addresses": [
                  {
                     "address1": "95 W. 5th Ave.",
                     "address2": "Apt. 437",
                     "city": "San Mateo",
                     "zip": "94402",
                     "region_name": "California",
                     "country_name": "United States",
                     "country_continent": "North America",
                     "country_currency": "USD"
                  }
               ],
               "store_credit_count": 1,
               "store_credit_total": 40
            }
         }
      ]
   }
}
```