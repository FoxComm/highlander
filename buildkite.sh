#!/bin/bash
make up
VAGRANT_UP_EXIT_CODE=${PIPESTATUS[0]}
make destroy

if [ "$VAGRANT_UP_EXIT_CODE" = "1" ]; then
    exit 1
else
    exit 0
fi
