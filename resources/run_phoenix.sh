#!/bin/bash

export PHOENIX_ENV=staging

kill $(ps aux | grep "phoenix.jar" | grep -v grep | awk '{print $2}')
java -jar phoenix.jar 2>&1 >> log/staging.log

