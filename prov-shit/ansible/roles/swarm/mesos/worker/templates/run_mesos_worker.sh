#!/bin/bash

WORK_DIR=/var/lib/mesos
IP=0.0.0.0
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib
export MESOS_EXECUTOR_ENVIRONMENT_VARIABLES="{\"LD_LIBRARY_PATH\": \"$LD_LIBRARY_PATH\"}"

mesos-slave --containerizers=docker,mesos \
            --docker_registry={{docker_registry}} \
            --ip=$IP \
            --work_dir=$WORK_DIR \
            --master={{zookeepers}}/mesos \
            --sandbox_directory=/var/lib/sandbox \
            --hostname=$IP
