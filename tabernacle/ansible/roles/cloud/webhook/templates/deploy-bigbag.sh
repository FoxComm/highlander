#!/usr/bin/env bash

set -ue

# Clone and build
git clone https://github.com/kpashka/bigbag.git
cd bigbag
make docker
make docker-push

# Trigger Marathon deployment
sed "s/NOW/$(date +%Y-%m-%d:%H:%M:%S)/g" storefront.json > latest.json

curl --header "Content-Type: application/json" \
    -d @latest.json \
    -XPUT http://{{cloud_demo_server}}/v2/apps/storefront

# Cleanup
cd ../
rm -rf bigbag
