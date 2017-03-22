# Hyperion

<img src="https://s-media-cache-ak0.pinimg.com/564x/89/85/6a/89856a96ee7c0ac941fcd76aeb369008.jpg" width="200"/>

Microservice to work with Amazon Marketplace Web Service

## Setup

**Create start.sh**

Rename `start.sh-template` to `start.sh` and add all needed ENV variables:

```bash
# DB
export HYPERION_DB_USER=hyperion
export HYPERION_DB_PASSWORD=''
export HYPERION_DB_NAME=hyperion_development
export HYPERION_DB_HOST=localhost

# for tests
export HYPERION_DB_TEST_NAME=hyperion_test

# for JWTAuth
export public_key=/full/path/to/public_key.pem

# AWS
export AWS_ACCESS_KEY_ID=aws_access_key
export AWS_SECRET_ACCESS_KEY=aws_secret

# MWS
export MWS_ACCESS_KEY_ID=mws_access_key
export MWS_SECRET_ACCESS_KEY=mws_secret

export PHOENIX_URL=https://appliance-10-240-0-12.foxcommerce.com
```

_IMPORTANT:_ Please keep in mind that AWS credentials differ from MWS. You can not use AWS data to access MWS and vice versa.

**Migrate DB**

Run flyway migrations

```bash
flyway -configFile=sql/flyway.conf -locations=filesystem:sql migrate
```

**Seed DB**

Add alias to host machine

```
sudo ifconfig lo0 alias 203.0.113.1
```

You can use any IP but that subnet reserved for tests by IANA.


Pull seed container

```
docker pull docker-stage.foxcommerce.com:5000/hyperion_seeder:master
```

or build it

```
docker build -t hyperion_seeder -f Dockerfile.seed --build-arg db_host=203.0.113.1 .
```

Add several lines to postgres config files:

*/usr/local/var/postgres/pg_hba.conf*

```
host    all             all             203.0.113.1/24          trust
```

*/usr/local/var/postgres/postgresql.conf*

```
listen_addresses='*'
```

Restart postgres

```
brew services restart postgres
```

Get container id

```
docker image ls
```

Run container

```
docker run -it --rm [container-id]
```

**Start application**

```bash
./start.sh
```


## Usage

Here are available urls:

```elixir
v1   GET    /v1/public/health  Check hyperion health
v1   GET    /v1/public/credentials  Get MWS credentials for exact client
v1   POST   /v1/public/credentials  Store new credentials
v1   PUT    /v1/public/credentials  Update credentials
v1   DELETE /v1/public/credentials  Remove credentials for specific client
v1   POST   /v1/public/products/:product_id/push  Push product to amazon
v1   GET    /v1/public/products/:product_id/result  Get push result for a product
v1   POST   /v1/public/products  Get products by ids and submit them to the Amazon MWS
v1   POST   /v1/public/products/by_asin  Add products by asin
v1   GET    /v1/public/products/search  Search products by code or query
v1   GET    /v1/public/products/find_by_asin/:asin  Searches product by ASIN code
v1   GET    /v1/public/products/categories/:asin  Returns categories for given asin
v1   GET    /v1/public/categories  Search for Amazon `department` and `item-type' by `node_path'
v1   GET    /v1/public/categories/suggest  Suggests category for product by title
v1   GET    /v1/public/categories/:node_id  Get category by amazon node_id
v1   GET    /v1/public/orders  Get all orders
v1   GET    /v1/public/orders/:order_id  Get order details
v1   GET    /v1/public/orders/:order_id/items  Get order items
v1   POST   /v1/public/prices  Submit prices for already submitted products
v1   POST   /v1/public/inventory  Submit inventory for already submitted products
v1   POST   /v1/public/images  submit images feed
v1   GET    /v1/public/submission_result/:feed_id  Check result of submitted feed
v1   POST   /v1/public/subscribe  Subscribe to notifications queue
v1   DELETE /v1/public/subscribe  Unubscribe from notifications queue
v1   GET    /v1/public/object_schema/:schema_name  Fetch object schema by name
v1   GET    /v1/public/object_schema/category/:category_id  Get object schema by amazon category id
v1   GET    /v1/public/object_schema  Get all available schemas
```

Get Postman collection [here](https://www.getpostman.com/collections/effaaa57089a01898f14)

Examples can be seen [here](https://github.com/FoxComm/highlander/wiki/Hyperion-Documentation)
