#!/usr/bin/env bash

# Include common library
source common.sh

# Define command-line arguments
DEBUG=false
if [[ $# -ge 1 ]] && [[ $1 == "-debug" ]]; then
    DEBUG=true
fi

DOCKER=false
if [[ $# -ge 1 ]] && [[ $1 == "-docker" ]]; then
    DOCKER=true
fi

# Define base branch via GitHub API
if [ "$BUILDKITE_PULL_REQUEST" != "false" ] ; then
    write "Fetching base branch for PR#$BUILDKITE_PULL_REQUEST via Github API..."
    GITHUB_BASE_URL=https://api.github.com/repos/FoxComm/highlander/pulls
    GITHUB_REQUEST_URL=$GITHUB_BASE_URL/$BUILDKITE_PULL_REQUEST?access_token=$GITHUB_API_TOKEN
    BASE_BRANCH_VALUE=$(curl -sS -XGET $GITHUB_REQUEST_URL | jq '.base.ref' | tr -d '"')
    BASE_BRANCH="origin/$BASE_BRANCH_VALUE"
else
    write "No pull request created, setting base branch to master"
    BASE_BRANCH="master"
fi

# Fetch origin quietly
git fetch origin -q

# Define projects
ALL_CHANGED=$(git diff --name-only $BASE_BRANCH...$BUILDKITE_COMMIT | xargs -I{} dirname {} | uniq)

# Make newlines the only separator
IFS=$'\n'
ALL_CHANGED=($ALL_CHANGED)
unset IFS

# Detect changed projects
CHANGED=()
if [[ ${#ALL_CHANGED[@]} -gt 0 ]]; then
    for CHANGE in ${ALL_CHANGED[@]}; do
        if [ $(contains "${PROJECTS[@]}" "$CHANGE") == "y" ]; then
            CHANGED+=($CHANGE)
        fi
    done
fi

# Build everything if script goes wrong
if [[ ${#CHANGED[@]} == 0 ]]; then
    write "No projects changed, building all by default"
    for PROJECT in ${PROJECTS[@]}; do
        CHANGED+=($PROJECT)
    done
fi

# Debug output
write "Changed projects (${#CHANGED[@]}):"
for item in "${CHANGED[@]}"
do
    write "\t ${item}"
done

# Build, test, dockerize, push
if [ "$DEBUG" = false ] ; then
    write "Building subprojects..."
    for PROJECT_DIR in "${CHANGED[@]}"
    do
        cd $PROJECT_DIR
        make build test
        if [ "$DOCKER" = true ] ; then
            make docker docker-push
        fi
        cd $HIGHLANDER_PATH
    done
fi
