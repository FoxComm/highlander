#!/usr/bin/env bash
set -euo pipefail

/isaac/isaac --http_port $PORT --public_key=$PUBLIC_KEY --db="host=$DB_HOST dbname=$DB_NAME user=$DB_USER"
