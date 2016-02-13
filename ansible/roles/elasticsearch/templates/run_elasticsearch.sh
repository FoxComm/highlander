#!/bin/bash

ELASTIC_ROOT=$1
export ES_HEAP_SIZE={{es_heap_size}}

cd $ELASTIC_ROOT
bin/elasticsearch
