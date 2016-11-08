#!/bin/bash

mesos-master \
    --quorum={{mesos_quorum}} \
    --zk={{zookeeper_service_consul}}/mesos \
    --work_dir={{mesos_work_dir}} \
    --cluster=fox \
    --ip={{inventory_hostname}} \
    --hostname={{inventory_hostname}}
