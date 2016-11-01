#!/bin/bash

CONTAINERIZERS=docker,mesos
WORK_DIR=/var/lib/mesos
SANDBOX_DIR=/var/lib/sandbox
IP=0.0.0.0

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib
export MESOS_EXECUTOR_ENVIRONMENT_VARIABLES="{\"LD_LIBRARY_PATH\": \"$LD_LIBRARY_PATH\"}"

mesos-slave --containerizers=$CONTAINERIZERS \
            --docker_registry={{docker_registry}} \
            --master={{zookeepers}}/mesos \
            --work_dir=$WORK_DIR \
            --sandbox_directory=/var/lib/sandbox \
            --ip=$IP \
            --hostname=$IP
