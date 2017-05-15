#!/usr/bin/env bash

# fail on unexported vars
set -ue

for fn in `./projects.sh`; do
    echo "Deploying application $fn"
    # cat $fn
done
