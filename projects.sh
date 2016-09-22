#!/bin/bash
VERBOSE=false
if [[ $# -ge 1 ]] && [[ $1 == "-verbose" ]]; then
    VERBOSE=true
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
    'api-js'
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
write "PR url: $FULL_URL"

# get base branch, against which PR is created
BASE_BRANCH=$(curl -sf $FULL_URL | jq -r '.[0] | .base.ref')
write "PR base branch: "$BASE_BRANCH

# if BASE_BRANCH is empty (no PR) - build against master
if [[ ! -z $BASE_BRANCH ]]; then
    BASE_BRANCH=master
fi
write "resolved BASE_BRANCH: $BASE_BRANCH"

# get current commit SHA
HEAD=$(git rev-parse HEAD)
write "HEAD: $HEAD"

# fetch origin
git fetch origin

# checkout BASE_BRANCH and pull it from origin
#write "Update $BASE_BRANCH..."
#write $(git checkout $BASE_BRANCH)
#write $(git pull origin $BASE_BRANCH)

# checkout back to HEAD
#write "Checkout back to HEAD..."
#write $(git checkout $HEAD)
#exit

# get common ancestor
write "Get common ancestor between $BASE_BRANCH and HEAD"
ANCESTOR=$(git merge-base $BASE_BRANCH HEAD)
write "Common ancestor: $ANCESTOR"

# get changed base paths in HEAD realtively to base path
write "Get diff between $ANCESTOR and $HEAD"
ALL_CHANGED=$(git diff --name-only $ANCESTOR..$HEAD | cut -d'/' -f1 | uniq)
write "changed base paths:\n"$ALL_CHANGED

# make newlines the only separator 
IFS=$'\n'
ALL_CHANGED=($ALL_CHANGED)
unset IFS

# get changed projects
CHANGED=("prov-shit")
write "building prov-shit by default"
for CHANGE in ${ALL_CHANGED[@]}; do
    if [[ ${PROJECTS[@]} =~ $CHANGE ]]; then
        CHANGED+=($CHANGE)
    fi
done

write "changed projects(${#CHANGED[@]}):"
echo ${CHANGED[@]}
