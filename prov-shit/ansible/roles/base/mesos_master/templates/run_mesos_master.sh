#!/bin/bash

export MESOS_WEBUI_DIR=/usr/local/share/mesos/webui
WORK_DIR=/var/lib/mesos
IP=`hostname -I | awk '{print $1}'`
ZOOKEEPER="zk://{{zookeeper_server}}"
QUORUM=2

/usr/local/sbin/mesos-master --quorum=$QUORUM --ip=$IP --work_dir=$WORK_DIR -zk=$ZOOKEEPER

