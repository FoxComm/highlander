#!/usr/bin/env bash

WORK_DIR=/var/lib/mesos
IP={{mesos_ip}}
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib
export MESOS_EXECUTOR_ENVIRONMENT_VARIABLES="{\"LD_LIBRARY_PATH\": \"$LD_LIBRARY_PATH\"}"

# So that agent can start with new config
rm -rf /var/lib/mesos/meta/slaves/latest

mesos-slave --containerizers=docker,mesos \
    --docker_registry={{docker_registry}} \
    --ip=$IP \
    --work_dir=$WORK_DIR \
    --master={{zookeepers}}/mesos \
    --sandbox_directory=/var/lib/sandbox \
    --hostname=$IP \
    --resources=file:///var/lib/mesos/resources.json \
    --executor_registration_timeout=10mins
