#!/bin/bash

KAFKA_ROOT=$1

cd $KAFKA_ROOT
bin/kafka-server-start.sh config/server.properties
