#!/usr/bin/env bash
set -euo pipefail

ELASTIC_ROOT=$1
unset ES_HEAP_SIZE

ES_JAVA_OPTS="-Xms{{es5_heap_size}} -Xmx{{es5_heap_size}}"
{% if jmx_enabled %}
ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote"
ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote.port={{es5_jmx_port}}"
ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote.local.only=false"
ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote.authenticate=false"
ES_JAVA_OPTS="${ES_JAVA_OPTS} -Dcom.sun.management.jmxremote.ssl=false"
{% endif %}
export ES_JAVA_OPTS

cd {{es5_path}}
bin/elasticsearch
