#!/bin/bash

sbt -Denv=staging -Dstaging.apiUrl=https://{{api_server}} -Dusers={{gatling_users}} -Dpause={{gatling_pause}} gatling:test
