#!/bin/bash

sbt -Denv=staging -Dusers={{gatling_users}} -Dpause={{gatling_pause}} test
