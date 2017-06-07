# product-activity

This project continuously queries number of active products from ES and
puts the value to Henhouse.

# Environment variables

| Variable name          |Default value   | Description                                                                                                  |
|:-----------------------|:----------------|:-------------------------------------------------------------------------------------------------------------|
| HENHOUSE_HOST          |               | Henhouse host |
| HENHOUSE_HTTP_PORT     |               | Port of HTTP interface of Henhouse. |
| HENHOUSE_INPUT_PORT    | 2003          | Port of Graphite interface of Henhouse |
| ELASTIC_INDEX          |               | Name of elasticsearch index. Typically "admin_1" |
| INTERVAL               | 60            | Polling interval in seconds|
| ELASTIC_URL            |               | Elasticsearch url  |
