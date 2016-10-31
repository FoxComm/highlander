#!/bin/bash

set -ue

WORK_DIR=/var/lib/mesos
IP=`hostname -I | awk '{print $1}'`
QUORUM={{mesos_quorum}}
CLUSTER_NAME={{mesos_cluster_name}}

export MESOS_WEBUI_DIR=/usr/share/mesos/webui

mesos-master --ip=$IP \
    --hostname=$IP \
    --zk={{zookeepers}}/mesos \
    --quorum=$QUORUM \
    --work_dir=$WORK_DIR \
    --cluster=$CLUSTER_NAME
