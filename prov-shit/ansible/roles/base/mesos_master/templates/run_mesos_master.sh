#!/bin/bash

WORK_DIR=/var/lib/mesos
IP=`hostname -I | awk '{print $1}'`
QUORUM={{mesos_quorum}}
CLUSTER_NAME={{mesos_cluster_name}}

export MESOS_WEBUI_DIR=/usr/share/mesos/webui

mesos-master --hostname=$IP \
    --ip=$IP \
    --quorum=$QUORUM \
    --zk={{zookeepers}}/mesos \
    --cluster=$CLUSTER_NAME \
    --work_dir=$WORK_DIR
