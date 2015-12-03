#!/bin/bash

KAFKA_ROOT=$1

cd $KAFKA_ROOT
bin/zookeeper-server-start.sh config/zookeeper.properties
