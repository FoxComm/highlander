#!/usr/bin/env bash

set -ue

# Clean checkout
rm -rf bigbag

# Clone repository
git clone https://github.com/kpashka/bigbag.git
cd bigbag

# Login to Docker Hub
source ./dockercfg
docker login --username=$DOCKER_LOGIN --password=$DOCKER_PASSWORD

# Build and push
make docker
make docker-push | tee

# Trigger Marathon deployment
sed "s/NOW/$(date +%Y-%m-%d:%H:%M:%S)/g" storefront.json > latest.json

curl --header "Content-Type: application/json" \
    -d @latest.json \
    -XPUT http://{{cloud_demo_server}}/v2/apps/storefront
