#!/bin/bash
make up
VAGRANT_UP_EXIT_CODE=${PIPESTATUS[0]}
make destroy

set -ue

if [ "$VAGRANT_UP_EXIT_CODE" = "1" ]; then
    echo "Failed to setup/provision GCE machine"
    exit 1
else
    echo "Developer appliance provision succeeded"
    exit 0
fi
