#!/usr/bin/env bash
# A script executed on BuildKite to test developer appliance provisioning on GCE

echo -e "--- Executing \033[33mmake up\033[0m"
make up
MAKE_UP_EXIT_CODE=$?

echo -e "--- Executing \033[33mmake destroy\033[0m"
make destroy

if [ $MAKE_UP_EXIT_CODE -eq 0 ]; then
    echo -e "--- Appliance creation \033[33msuccess\033[0m"
    echo "make up exit code: $MAKE_UP_EXIT_CODE"
    exit 0
else
    echo -e "--- Appliance creation \033[33mfailure\033[0m"
    echo "make up exit code: $MAKE_UP_EXIT_CODE"
    exit 1
fi
