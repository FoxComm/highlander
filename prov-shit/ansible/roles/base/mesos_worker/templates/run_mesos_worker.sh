#!/bin/bash

WORK_DIR=/var/lib/mesos
IP=`hostname -I | awk '{print $1}'`

LD_LIBRARY_PATH=$LD_LIBRARY_PATH:/usr/local/lib /usr/local/sbin/mesos-slave --docker_registry={{docker_registry}} --ip=$IP --work_dir=$WORK_DIR --master={{mesos_master}} --sandbox_directory=/var/lib/sandbox --image_providers=APPC,DOCKER --hostname=$IP --isolation=posix/cpu,posix/mem,docker/runtime

