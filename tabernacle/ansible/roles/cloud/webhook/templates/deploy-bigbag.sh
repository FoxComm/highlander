#!/usr/bin/env bash

set -ue

# Clean checkout
rm -rf bigbag

# Clone repository
git clone https://github.com/kpashka/bigbag.git
cd bigbag

# Login to Docker Hub
source {{webhook_dir}}/.dockercfg
docker login --username=$DOCKER_LOGIN --password=$DOCKER_PASSWORD

# Build and push
make docker
make docker-push | tee

# Trigger Marathon deployment
sed "s/NOW/$(date +%Y-%m-%d:%H:%M:%S)/g" {{webhook_dir}}/storefront.json > {{webhook_dir}}/latest.json

curl --header "Content-Type: application/json" \
    -d @{{webhook_dir}}/latest.json \
    -XPUT http://{{cloud_demo_server}}/v2/apps/storefront
