#!/bin/bash

# Defines which projects should be rebuilt from descriptions in GitHub PR
# Format: `PROJECTS=(project1 project2)`

set -ue

DEFAULT="ashes firebrand fox-notifications green-river integration-tests isaac middlewarehouse phoenix-scala prov-shit"
BASE_URL="https://api.github.com/repos/FoxComm/highlander"
FULL_URL="$BASE_URL/pulls?head=FoxComm:$BUILDKITE_BRANCH&access_token=$GITHUB_API_TOKEN"
OUTPUT=$(curl -sf $FULL_URL | grep -o "PROJECTS=(.*)" | tr -d '(' | tr -d 'PROJECTS=' | tr -d ')')

if [ -z "$OUTPUT" ]
then
    echo $DEFAULT
else
    echo $OUTPUT
fi
