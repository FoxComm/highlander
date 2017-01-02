#!/usr/bin/env bash

FLYWAY="/usr/local/share/flyway/flyway -configFile=sql/flyway.conf -locations=filesystem:sql/"
CONNECTION_STRING=$(cat sql/flyway.conf | grep 'flyway.url' | awk -F ':' '{print $3}')
PQL="psql postgresql:$CONNECTION_STRING"
ARTIFACT=target/scala-2.11/phoenix-scala-assembly-1.0.jar

#download artifact
buildkite-agent artifact download "$ARTIFACT" ./

killall bottledwater

#since we don't have bottledwater provisioned yet for db
#we can't use flyway yet.
echo "select pg_drop_replication_slot('phoenix');" | $PQL
echo 'drop extension bottledwater;' | $PQL

#migrate db
$FLYWAY clean
$FLYWAY migrate

#seed db
java -cp $ARTIFACT utils.seeds.Seeds

#run phoenix
mkdir -p log
killall java || true
sleep 1
export PHOENIX_ENV=staging; nohup java -jar $ARTIFACT 2>&1 >> log/phoenix.log &
sleep 1
ps aux | grep java || true


#Start bottledwater
echo 'create extension bottledwater;' | $PQL

#This script is on the stage machine and not yet checked in.
#It will be part once bottledwater is packaged and provisioned via vagrant
nohup /usr/local/bin/run_bottledwater.sh &
