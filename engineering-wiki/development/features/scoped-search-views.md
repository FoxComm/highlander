# Scoped Search Views

## Steps to Implement

* Create search view table in database

Dates must be of `json_timestamp` type;
You can convert data from `timestamp` to `json_timestamp` using `to_json_timestamp(field)` or `to_char(field, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')` functions;
Table must have scope field of `ltree` type;

* Create triggers for 'create', 'update', 'delete' operations, that will copy data from tables that holds data in relation model into search view table

One of the most convenient ways to check and debug triggers is using of `explain analyze verrbose` modifire for sql queries;

* Create index mapping in GreenRiver

Be careful, ElasticSearch index cannot have fields with same name but different types in mappings;

* Wire index with kafka topic

This is achieved by modifing 2 files:
- `application.conf` in GreenRiver by adding serach view table name to `kafka.scoped.indices` property;
- `consumers.Workers` object: mapping of kafka topic to search view class must be added to `topicTransformers` method;

* Configure scope application th the requests in nginx

The template for nginx configuration can be found in `ansible/roles/dev/balancer/templates/nginx.conf.j2` file.
You have to `search_view_name = 1` to `scoped_views` array in `init_by_lua` function.

## Deployment Process

* Rebuild GreenRiver docker container
* Change nginx configuration file
* Stop bottledwater service and services that are connected to the source database
* Migrate databases
* Start bottlewater service and services that are connected to the source databse
* Deploy new image of greenriver to Mesos cluster
