#!/usr/bin/env bash

ELASTIC_ROOT=$1
unset ES_HEAP_SIZE

ES_JAVA_OPTS="-Xms{{es_heap_size}} -Xmx{{es_heap_size}}"
ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote"
ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote.port={{consul_ports.es_jmx}}"
ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote.local.only=false"
ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote.authenticate=false"
ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote.ssl=false"
export ES_JAVA_OPTS


cd {{es_path}}
bin/elasticsearch
