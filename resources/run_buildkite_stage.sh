#!/bin/bash

ARTIFACT=target/scala-2.11/phoenix-scala-assembly-1.0.jar

#download artifact
buildkite-agent artifact download "$ARTIFACT" ./

#migrate db
/usr/local/share/flyway/flyway -configFile=sql/flyway.conf -locations=filesystem:sql clean
/usr/local/share/flyway/flyway -configFile=sql/flyway.conf -locations=filesystem:sql migrate

#seed db
java -cp $ARTIFACT utils.seeds.Seeds

#run phoenix
mkdir -p log
killall java || true
sleep 1
export PHOENIX_ENV=staging; nohup java -jar $ARTIFACT 2>&1 >> log/phoenix.log &
sleep 1
ps aux | grep java || true
