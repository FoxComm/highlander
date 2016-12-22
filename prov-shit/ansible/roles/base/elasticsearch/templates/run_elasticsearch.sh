#!/usr/bin/env bash

ELASTIC_ROOT=$1
export ES_HEAP_SIZE={{es_heap_size}}

ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote"
ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote.port={{es_jmx_port}}"
ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote.local.only=false"
ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote.authenticate=false"
ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote.ssl=false"
export ES_JAVA_OPTS


cd $ELASTIC_ROOT
bin/elasticsearch
