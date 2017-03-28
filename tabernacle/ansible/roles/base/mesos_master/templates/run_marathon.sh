#!/usr/bin/env bash

/usr/bin/marathon --master {{zookeepers}}/mesos \
    --zk {{zookeepers}}/marathon \
    --mesos_authentication \
    --mesos_authentication_principal=marathon \
    --mesos_authentication_secret={{mesos_pass}} \
    --mesos_role=marathon \
