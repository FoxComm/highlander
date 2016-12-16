#!/bin/bash
# A script executed on BuildKite to test developer appliance provisioning on GCE

export VAGRANT_DEFAULT_PROVIDER=google
export eval `cat ./.env.local`

echo -e "--- Executing \033[33mvagrant up\033[0m"
vagrant up --provider=google appliance
VAGRANT_UP_EXIT_CODE=$?

echo -e "--- Executing \033[33mvagrant destroy\033[0m"
vagrant destroy appliance --force

if [ $VAGRANT_UP_EXIT_CODE -eq 0 ]; then
    echo -e "--- Appliance creation \033[33msuccess\033[0m"
    echo "vagrant up exit code: $VAGRANT_UP_EXIT_CODE"
    exit 0
else
    echo -e "--- Appliance creation \033[33mfailure\033[0m"
    echo "vagrant up exit code: $VAGRANT_UP_EXIT_CODE"
    exit 1
fi
