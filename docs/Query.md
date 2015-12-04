# Query

How to query `phoenix` index in Elasticsearch.

Jump to:
* [Structure](#structure)
* [Filtering](#filtering)
* [Sorting](#sorting)

## Structure

Currently, we store multiple types in Elasticsearch:
* `countries`
* `regions` 
* `customers_search_view`

Each type has a list of allowed fields (others will be filtered by consumer) with different field types and indexing rules.

Most general examples are:
* `integer`, `long` - used for numeric comparsions, e.g. [Range Query](https://www.elastic.co/guide/en/elasticsearch/reference/1.7/query-dsl-range-query.html)
* `date` - also filtered by [Range Query](https://www.elastic.co/guide/en/elasticsearch/reference/1.7/query-dsl-range-query.html)
* `string`
  * `not_analyzed` - used for ADT comparsion, when we know predefined range of values (e.g. `status`, `currency`)
  * `autocomplete` - custom analyzer used for partial match, based on [Edge NGram Tokenizer](https://www.elastic.co/guide/en/elasticsearch/reference/1.7/analysis-edgengram-tokenizer.html)
* `boolean` - simplified ADT with two possible values, usually filtered by [Term Filter](https://www.elastic.co/guide/en/elasticsearch/reference/1.7/query-dsl-term-query.html)

## Filtering

Query below returns a list of first 10 (seen `from` and `size` fields) customers, that:
* Have `Adil` substring in his name
* Are not blacklisted
* Joined after December 3rd, 2015
* Has 1 or more orders with any status
* Has at least one order with "shipped" status and placed date after December 3rd, 2015

To query nested objects in last expression, we use [Nested Filter](https://www.elastic.co/guide/en/elasticsearch/reference/2.0/query-dsl-nested-query.html)
All conditions must be satisfied, when using [And Filter](https://www.elastic.co/guide/en/elasticsearch/reference/1.7/query-dsl-and-filter.html), 
Also take a look at [Or Filter](https://www.elastic.co/guide/en/elasticsearch/reference/1.4/query-dsl-or-filter.html), if necessary.

##### Request

```json
GET phoenix/customers_search_view/_search
{
    "query": {
        "filtered": {
            "filter": {
                "and": [
                    {"bool": {"must": {"query": {"match": {"name": "Adil"}}}}},
                    {"term": {"is_blacklisted": false}},
                    {"range": {"date_joined": {"gt": "2015-12-03"}}},
                    {"range": {"order_count": {"gte": 1}}},
                    {
                        "nested": {
                            "path": "orders",
                            "query": {
                                "bool": {
                                    "must" : [
                                         {"term": {"status": "shipped"}},
                                         {"range": {"date_placed": {"gt": "2015-12-03"}}}
                                    ]
                                }
                            }
                        }
                    }
                ]
            }    
        }
    },
    "from": 0,
    "size": 10
}
```

##### Response

```json
{
   "took": 4,
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
            "_type": "customers_search_view",
            "_id": "2",
            "_score": 1,
            "_source": {
               "id": 2,
               "name": "Adil Wali",
               "email": "adil@adil.com",
               "is_disabled": true,
               "is_guest": false,
               "is_blacklisted": false,
               "date_joined": "2015-12-04",
               "revenue": 0,
               "rank": 0,
               "store_credit_count": 0,
               "store_credit_total": 0,
               "order_count": 1,
               "orders": [
                  {
                     "reference_number": "BR10005",
                     "status": "shipped",
                     "date_placed": "2015-12-04"
                  }
               ],
               "purchased_items_count": 2,
               "purchased_items": [
                  {
                     "sku": "SKU-SHH",
                     "name": "Sharkling",
                     "price": 1500
                  },
                  {
                     "sku": "SKU-YAX",
                     "name": "Flonkey",
                     "price": 3300
                  }
               ],
               "shipping_addresses": [
                  {
                     "address1": "3104 Canterbury Court",
                     "address2": "★ ★ ★",
                     "city": "Cornelius",
                     "zip": "28031",
                     "region_name": "North Carolina",
                     "country_name": "United States",
                     "country_continent": "North America",
                     "country_currency": "USD"
                  },
                  {
                     "address1": "3345 Orchard Lane",
                     "address2": null,
                     "city": "Avon Lake",
                     "zip": "44012",
                     "region_name": "Ohio",
                     "country_name": "United States",
                     "country_continent": "North America",
                     "country_currency": "USD"
                  }
               ],
               "billing_addresses": [
                  {
                     "address1": "3564 Haymond Rocks Road",
                     "address2": null,
                     "city": "Grants Pass",
                     "zip": "97526",
                     "region_name": "Oregon",
                     "country_name": "United States",
                     "country_continent": "North America",
                     "country_currency": "USD"
                  }
               ],
               "saved_for_later_count": 0,
               "save_for_later": []
            }
         }
      ]
   }
}
```

## Sorting

To perform [sorting](https://www.elastic.co/guide/en/elasticsearch/guide/current/_sorting.html), just add custom sorting fields in `sort` field.

Example:

```json
GET phoenix/customers_search_view/_search
{
    "query": {
        "match_all": {}
    },
    "sort": {
        "billing_addresses.address1": "asc",
        "date_joined": "asc",
        "name": "asc",
        "_score": "desc"
    }
}
```