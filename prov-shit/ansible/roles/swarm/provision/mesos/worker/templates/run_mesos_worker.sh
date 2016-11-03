#!/bin/bash

IP=`hostname -I | awk '{print $1}'`

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib
export MESOS_EXECUTOR_ENVIRONMENT_VARIABLES="{\"LD_LIBRARY_PATH\": \"$LD_LIBRARY_PATH\"}"

mesos-slave --containerizers=docker,mesos \
            --docker_registry={{docker_registry}} \
            --master={{zookeepers}}/mesos \
            --work_dir={{mesos_work_dir}} \
            --sandbox_directory={{mesos_sandbox_dir}} \
            --ip=$IP \
            --hostname=$IP
