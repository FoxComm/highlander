#!/bin/bash
export VAGRANT_DEFAULT_PROVIDER=google
export eval `cat ./.env.local`

echo -e "--- Executing \033[33mvagrant up\033[0m"
vagrant up --provider=google appliance
VAGRANT_UP_EXIT_CODE=$?

echo -e "--- Executing \033[33mvagrant destroy\033[0m"
vagrant destroy appliance --force

echo "--- Final report"

if [ $VAGRANT_UP_EXIT_CODE -ne 0 ]; then
    echo -e "Developer appliance creation \033[33msuccess\033[0m"
    exit 0
else
    echo -e "Developer appliance creation \033[33mfailure\033[0m"
    exit 1
fi
