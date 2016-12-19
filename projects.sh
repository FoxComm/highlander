#!/bin/bash
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
    'api-docs'
    'api-js'
    'ashes'
    'engineering-wiki'
    'firebrand'
    'green-river'
    'isaac'
    'marketplace'
    'marketplace-ui'
    'messaging'
    'middlewarehouse'
    'phoenix-scala'
    'prov-shit'
    'solomon'
)

# fetch origin
git fetch origin

if $ALL; then
    echo ${PROJECTS[@]}
else
    # get the current branch name
    CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
    ALL_CHANGED=$(git diff --name-only master...$CURRENT_BRANCH | cut -d'/' -f1 | uniq)

    # make newlines the only separator
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
