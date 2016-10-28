#!/bin/bash

QUORUM={{mesos_quorum}}
WORK_DIR=/var/lib/mesos
IP=0.0.0.0

mesos-master \
    --quorum=$QUORUM \
    --zk={{zookeepers}}/mesos \
    --work_dir=$WORK_DIR \
    --cluster=fox \
    --ip=$IP \
    --hostname=$IP
