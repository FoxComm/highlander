#!/usr/bin/env bash

WORK_DIR=/var/lib/mesos
SANDBOX_DIR=/var/lib/sandbox
IP=`hostname -I | awk '{print $1}'`

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib
export MESOS_EXECUTOR_ENVIRONMENT_VARIABLES="{\"LD_LIBRARY_PATH\": \"$LD_LIBRARY_PATH\"}"

mesos-slave --hostname=$IP \
    --ip=$IP \
    --master={{zookeepers}}/mesos \
    --containerizers=docker,mesos \
    --docker_registry={{docker_registry}} \
    --work_dir=$WORK_DIR \
    --sandbox_directory=/$SANDBOX_DIR
