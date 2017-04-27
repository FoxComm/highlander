#!/usr/bin/env bash


WORK_DIR=/var/lib/mesos
IP=`hostname -I | awk '{print $1}'`
export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib
export MESOS_EXECUTOR_ENVIRONMENT_VARIABLES="{\"LD_LIBRARY_PATH\": \"$LD_LIBRARY_PATH\"}"

#so that agent can start with new config
rm -f /var/lib/mesos/meta/slaves/latest

mesos-slave --containerizers=docker,mesos --docker_registry={{docker_registry}} --ip=$IP --work_dir=$WORK_DIR --master={{zookeepers}}/mesos --sandbox_directory=/var/lib/sandbox --hostname=$IP --resources=file:///var/lib/mesos/resources.json
