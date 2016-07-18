#!/bin/bash

sbt -Denv=staging -Dstaging.apiUrl=http://{{api_server}}/api -Dusers={{gatling_users}} -Dpause={{gatling_pause}} test
