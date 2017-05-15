#!/usr/bin/env bash

# fail on unexported vars
set -ue

cd tabernacle

for app in `./projects.sh`; do
    echo "Deploying application $app"
    ansible-playbook -v -i inventory/static/dev ansible/goldrush_update_app.yml --extra-vars "app_name=$app branch_name=$DEPLOY_TAG_NAME auto_build=n instance_ip=$DEPLOY_INSTANCE_IP"
done
