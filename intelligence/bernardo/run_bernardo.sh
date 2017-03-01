#!/bin/bash

/bernardo/bernardo --http_port $PORT --http2_port $PORT1 --db="host=$DB_HOST dbname=$DB_NAME user=$DB_USER"
