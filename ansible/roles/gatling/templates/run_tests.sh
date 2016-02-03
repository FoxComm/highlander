#!/bin/bash

sbt -Denv=staging -Dstaging.phoenixUrl=http://{{phoenix_server}} -Dstaging.elasticUrl=http://{{search_server_http}} -Dusers=5 -Dpause=1 test
