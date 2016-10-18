#!/bin/bash

/usr/local/bin/mesos-consul --healthcheck --log-level=info --zk=zk://{{zookeepers}}/mesos
