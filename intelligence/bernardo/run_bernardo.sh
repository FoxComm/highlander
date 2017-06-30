#!/bin/bash
set -euo pipefail

/bernardo/bernardo --http_port $PORT --http2_port $PORT1 --db="host=$DB_HOST dbname=$DB_NAME user=$DB_USER" 2>&1 | tee /logs/bernardo.log
