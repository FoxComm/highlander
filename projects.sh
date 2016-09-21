#!/bin/bash

# fail on unexported vars
set -ue

# define buildable projects array
PROJECTS=(
    'ashes'
    'firebrand'
    'fox-notifications'
    'green-river'
    'integration-tests'
    'isaac'
    'middlewarehouse'
    'phoenix-scala'
    'prov-shit'
)

BASE_URL="https://api.github.com/repos"

# get ORG/REPO string from url and split it
FULL_REPO=$(git config --get remote.origin.url | cut -d ':' -f2 | cut -d '.' -f1)
ORG=$(echo $FULL_REPO | cut -d '/' -f1)
REPO=$(echo $FULL_REPO | cut -d '/' -f2)

# get base branch through PR info
FULL_URL="$BASE_URL/$FULL_REPO/pulls?head=$ORG:$BUILDKITE_BRANCH&access_token=$GITHUB_API_TOKEN"

#BASE_BRANCH=$(curl -sf $FULL_URL | jq -r '.[0] | .base.ref')
BASE_BRANCH=master

# get changed base paths in HEAD realtively to base path
ALL_CHANGED=$(git diff --name-only $BASE_BRANCH..HEAD | cut -d'/' -f1 | uniq)

# make newlines the only separator 
IFS=$'\n'
ALL_CHANGED=($ALL_CHANGED)
unset IFS

# get changed projects
CHANGED=()
for CHANGE in ${ALL_CHANGED[@]}; do
    if [[ ${PROJECTS[@]} =~ $CHANGE ]]; then
        CHANGED+=($CHANGE)
    fi
done

echo ${CHANGED[@]}
