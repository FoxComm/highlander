#!/bin/bash

#JAR=$1
#if [ ! -e $JAR ]; then sbt assembly; fi
#nohup scala $JAR -Denv=localhost consume
sbt -Denv=localhost clean consume
