#!/usr/bin/env bash
# A script executed on BuildKite to test developer appliance provisioning on GCE

make up
MAKE_UP_EXIT_CODE=$?
make destroy

if [ $MAKE_UP_EXIT_CODE -eq 0 ]; then
    echo -e "--- Appliance creation \033[33msuccess\033[0m"
    echo "make up exit code: $MAKE_UP_EXIT_CODE"
    exit 0
else
    echo -e "--- Appliance creation \033[41mfailure\033[0m"
    echo "make up exit code: $MAKE_UP_EXIT_CODE"
    exit 1
fi
