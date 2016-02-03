#!/bin/bash

sbt -Denv=staging -Dstaging.phoenixUrl=http://{{phoenix_server}} -Dstaging.elasticUrl=http://{{search_server_http}} -Dusers={{gatling_users}} -Dpause={{gatling_pause}} test
