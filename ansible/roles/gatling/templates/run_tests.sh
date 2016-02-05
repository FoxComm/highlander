#!/bin/bash

sbt -Denv=staging -Dstaging.apiUrl=http://{{phoenix_server}} -Dusers={{gatling_users}} -Dpause={{gatling_pause}} test
