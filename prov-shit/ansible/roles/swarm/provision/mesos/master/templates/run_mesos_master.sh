#!/bin/bash

IP=`hostname -I | awk '{print $1}'`

mesos-master \
    --quorum={{mesos_quorum}} \
    --zk={{zookeepers}}/mesos \
    --work_dir={{mesos_work_dir}} \
    --cluster=fox \
    --ip=$IP \
    --hostname=$IP
