#!/bin/bash

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib
export MESOS_EXECUTOR_ENVIRONMENT_VARIABLES="{\"LD_LIBRARY_PATH\": \"$LD_LIBRARY_PATH\"}"

mesos-slave --containerizers=docker,mesos \
            --docker_registry={{docker_registry}} \
            --master={{zookeeper_service_consul}}/mesos \
            --work_dir={{mesos_work_dir}} \
            --sandbox_directory={{mesos_sandbox_dir}} \
            --ip={{inventory_hostname}} \
            --hostname={{inventory_hostname}}
