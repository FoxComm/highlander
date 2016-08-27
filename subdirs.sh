#!/bin/bash
DEFAULT="ashes firebird green-river isaac phoenix-scala prov-shit integration-tests fox-notifications middlewarehouse"
OUTPUT=$(curl -sf https://api.github.com/repos/FoxComm/highlander/pulls\?head\=FoxComm:$BUILDKITE_BRANCH\&access_token\=$GITHUB_API_TOKEN | grep -o "\`SUBDIRS=.*\`" | tr -d '`' | tr -d 'SUBDIRS=')

if [ -z "$OUTPUT" ]
then
    export SUBDIRS=$DEFAULT
else
    export SUBDIRS=$OUTPUT
fi