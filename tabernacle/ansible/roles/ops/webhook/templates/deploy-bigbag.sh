#!/usr/bin/env bash

set -ue

# Clean checkout
rm -rf {{clone_dirname}}

# Clone repository
git clone {{target_repo_url}}
cd {{clone_dirname}}

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
    -XPUT http://{{marathon_address}}/v2/apps/storefront
