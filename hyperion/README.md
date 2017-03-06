# Hyperion

<img src="https://s-media-cache-ak0.pinimg.com/564x/89/85/6a/89856a96ee7c0ac941fcd76aeb369008.jpg" width="200"/>

Microservice to work with Amazon Marketplace Web Service

## Setup

###DB

```bash
export HYPERION_DB_USER=hyperion
export HYPERION_DB_PASSWORD=''
export HYPERION_DB_NAME=hyperion_development
export HYPERION_DB_HOST=localhost
```

###AWS

AWS used for getting notification with SQS

```bash
export AWS_ACCESS_KEY_ID=aws_access_key
export AWS_SECRET_ACCESS_KEY=aws_secret
```
###MWS

```bash
export MWS_ACCESS_KEY_ID=mws_access_key
export MWS_SECRET_ACCESS_KEY=mws_secret
```

###Phoenix

```bash
export PHOENIX_URL=https://appliance-10-240-0-12.foxcommerce.com
```

_IMPORTANT:_ Please keep in mind that AWS credentials differ from MWS. You can not use AWS data to access MWS and vice versa.

## Usage

Here are available urls:

```elixir
v1   GET    /v1/health  Check hyperion health
v1   GET    /v1/credentials/:client_id  Get MWS credentials for exact client
v1   POST   /v1/credentials  Store new credentials
v1   PUT    /v1/credentials/:client_id  Update credentials
v1   POST   /v1/products  Get products by ids and submit them to the Amazon MWS
v1   POST   /v1/products/by_asin  Add products by asin
v1   GET    /v1/products/search  Search products by code or query
v1   GET    /v1/products/find_by_asin/:asin  Searches product by ASIN code
v1   GET    /v1/products/categories/:asin  Returns categories for given asin
v1   GET    /v1/categories  Search for Amazon `department` and `item-type' by `node_path'
v1   GET    /v1/categories/suggest  Suggests category for product by title
v1   GET    /v1/orders  Get all orders
v1   POST   /v1/prices  Submit prices for already submitted products
v1   POST   /v1/inventory  Submit inventory for already submitted products
v1   POST   /v1/images  submit images feed
v1   GET    /v1/submission_result/:feed_id  Check result of submitted feed
v1   POST   /v1/subscribe  Subscribe to notifications queue
v1   DELETE /v1/subscribe  Unubscribe from notifications queue
```

Get Postman collection [here](https://www.getpostman.com/collections/effaaa57089a01898f14)

Examples can be seen [here](https://github.com/FoxComm/highlander/tree/master/engineering-wiki/hyperion)
