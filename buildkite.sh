#!/bin/bash
export VAGRANT_DEFAULT_PROVIDER=google
export eval `cat ./.env.local`

echo -e "--- Executing \033[33mvagrant up\033[0m"
vagrant up --provider=google appliance
VAGRANT_UP_EXIT_CODE=$?

echo -e "--- Executing \033[33mvagrant destroy\033[0m"
vagrant destroy appliance --force

if [ $VAGRANT_UP_EXIT_CODE -eq 1 ]; then
    echo "Failed to setup/provision GCE machine"
    exit 1
else
    echo "Developer appliance provision succeeded"
    exit 0
fi
