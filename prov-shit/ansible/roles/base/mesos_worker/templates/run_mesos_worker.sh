#!/bin/bash

set -ue

SANDBOX_DIR=/var/lib/sandbox
WORK_DIR=/var/lib/mesos
IP=`hostname -I | awk '{print $1}'`

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib
export MESOS_EXECUTOR_ENVIRONMENT_VARIABLES="{\"LD_LIBRARY_PATH\": \"$LD_LIBRARY_PATH\"}"

mesos-slave --ip=$IP \
    --master={{zookeepers}}/mesos \
    --docker_registry={{docker_registry}} \
    --containerizers=docker,mesos \
    --work_dir=$WORK_DIR \
    --sandbox_directory=$SANDBOX_DIR \
    --hostname=$IP
