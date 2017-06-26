#!/usr/bin/env bash
set -euo pipefail

export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib
export MESOS_EXECUTOR_ENVIRONMENT_VARIABLES="{\"LD_LIBRARY_PATH\": \"$LD_LIBRARY_PATH\"}"

WORK_DIR=/var/lib/mesos
LOG_DIR=/var/log/mesos
SANDBOX_DIR=/var/lib/sandbox
IP=`hostname -i`

{% if is_appliance or is_staging %}
mesos-slave --hostname=$IP \
    --ip=$IP \
    --master={{zookeepers}}/mesos \
    --containerizers=docker,mesos \
    --docker_registry={{docker_registry}} \
    --work_dir=$WORK_DIR \
    --log_dir=$LOG_DIR \
    --sandbox_directory=$SANDBOX_DIR \
    --resources=file:///var/lib/mesos/resources.json \
    --executor_registration_timeout=10mins
{% else %}
mesos-slave --hostname=$IP \
    --ip=$IP \
    --master={{zookeepers}}/mesos \
    --containerizers=docker,mesos \
    --docker_registry={{docker_registry}} \
    --work_dir=$WORK_DIR \
    --log_dir=$LOG_DIR \
    --sandbox_directory=$SANDBOX_DIR \
    --executor_registration_timeout=10mins
{% endif %}
