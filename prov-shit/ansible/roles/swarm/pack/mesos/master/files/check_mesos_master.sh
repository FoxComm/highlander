#!/bin/bash

IP=`hostname -I | awk '{print $1}'`
curl http://$IP:{{mesos_port}}
