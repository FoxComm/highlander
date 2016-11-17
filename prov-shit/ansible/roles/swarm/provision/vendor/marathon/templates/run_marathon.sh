#!/bin/bash

marathon --master {{zookeeper_service_consul}}/mesos \
         --zk {{zookeeper_service_consul}}/marathon
