#!/usr/bin/env bash

# Fail on unexported vars
set -ue

# Define buildable projects array
PROJECTS=(
    'ashes'
    'data-import'
    'demo/peacock'
    'developer-portal'
    'green-river'
    'hyperion'
    'intelligence/anthill'
    'intelligence/bernardo'
    'intelligence/consumers/digger-sphex'
    'intelligence/consumers/orders-anthill'
    'intelligence/consumers/orders-reviews'
    'intelligence/consumers/orders-sphex'
    'intelligence/consumers/product-activity'
    'intelligence/eggcrate'
    'intelligence/river-rock'
    'intelligence/suggester'
    'intelligence/user-simulation'
    'isaac'
    'messaging'
    'middlewarehouse'
    'middlewarehouse/common/db/seeds'
    'middlewarehouse/consumers/capture'
    'middlewarehouse/consumers/customer-groups'
    'middlewarehouse/consumers/gift-cards'
    'middlewarehouse/consumers/shipments'
    'middlewarehouse/consumers/shipstation'
    'middlewarehouse/elasticmanager'
    'onboarding/service'
    'onboarding/ui'
    'phoenix-scala'
    'phoenix-scala/seeder'
    'search-service'
    'solomon'
    'tabernacle/docker/neo4j'
    'tabernacle/docker/neo4j_reset'
)

# Save Highlander directory
HIGHLANDER_PATH=$PWD

# Define helper functions
function write() {
    if $DEBUG; then
        echo -e "[BUILDER]" $1
    fi
}

function contains() {
    local n=$#
    local value=${!n}
    for ((i=1;i < $#;i++)) {
        if [ "${!i}" == "${value}" ]; then
            echo "y"
            return 0
        fi
    }
    echo "n"
    return 1
}

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
    write "List of changed projects:"
    for CHANGE in ${ALL_CHANGED[@]}; do
        if [ $(contains "${PROJECTS[@]}" "$CHANGE") == "y" ]; then
            write "\t${CHANGE}"
            CHANGED+=($CHANGE)
        fi
    done
fi

# Build everything if on master, quit if on branch
if [[ ${#CHANGED[@]} == 0 ]]; then
    if [ "$BUILDKITE_PIPELINE_SLUG" == "highlander-master" ] ; then
        write "Rebuilding everything"
        for PROJECT in ${PROJECTS[@]}; do
            CHANGED+=($PROJECT)
        done
    else
        write "No projects changed, nothing to build"
        exit 0;
    fi
fi

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
