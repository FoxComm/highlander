#!/bin/bash
export VAGRANT_DEFAULT_PROVIDER=google
export eval `cat ./.env.local`

vagrant up --provider=google appliance
VAGRANT_UP_EXIT_CODE=${PIPESTATUS[0]}
vagrant destroy appliance --force

set -ue

if [ "$VAGRANT_UP_EXIT_CODE" = "1" ]; then
    echo "Failed to setup/provision GCE machine"
    exit 1
else
    echo "Developer appliance provision succeeded"
    exit 0
fi
