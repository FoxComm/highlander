#!/bin/bash

kill $(ps aux | grep "phoenix.jar" | grep -v grep | awk '{print $2}')
java -jar phoenix.jar 2>&1 >> log/phoenix.log

