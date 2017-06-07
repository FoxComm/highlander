#!/usr/bin/env bash

####################################
# DEPRECATED                       #
####################################

VERBOSE=false
if [[ $# -ge 1 ]] && [[ $1 == "-verbose" ]]; then
    VERBOSE=true
fi

ALL=false
if [[ $# -ge 1 ]] && [[ $1 == "-all" ]]; then
    ALL=true
fi

write() {
    if $VERBOSE; then
        echo -e $1
    fi
}

# fail on unexported vars
set -ue

# define buildable projects array
PROJECTS=(
    'ashes'
    'data-import'
    'developer-portal'
    'demo'
    'intelligence'
    'green-river'
    'isaac'
    'messaging'
    'middlewarehouse'
    'phoenix-scala'
    'tabernacle'
    'solomon'
    'hyperion'
)

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

# fetch origin
git fetch origin

if $ALL; then
    echo ${PROJECTS[@]}
else
    ALL_CHANGED=$(git diff --name-only $BASE_BRANCH...$BUILDKITE_COMMIT | cut -d'/' -f1 | uniq)

    # Make newlines the only separator
    IFS=$'\n'
    ALL_CHANGED=($ALL_CHANGED)
    unset IFS

    # get changed projects
    CHANGED=()
    if [[ ${#ALL_CHANGED[@]} -gt 0 ]]; then
        for CHANGE in ${ALL_CHANGED[@]}; do
            if [[ ${PROJECTS[@]} =~ $CHANGE ]]; then
                CHANGED+=($CHANGE)
            fi
        done
    fi

    if [[ ${#CHANGED[@]} == 0 ]]; then
        write "No projects changed, building all by default"
        for PROJECT in ${PROJECTS[@]}; do
            CHANGED+=($PROJECT)
        done
    fi

    write "changed projects(${#CHANGED[@]}):"
    echo ${CHANGED[@]}
fi
