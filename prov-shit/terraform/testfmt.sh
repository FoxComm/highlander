#!/usr/bin/env bash
set -ue

unformatted_files=$(terraform fmt)

if [[ $? != 0 ]]; then
    echo "Terraform format command failed."
    exit 1;
elif [[ $unformatted_files ]]; then
    echo "Unformatted Terraform files found.";
    echo "Please run Terraform format";
    exit 1;
else
    echo "Terraform formatting check passed";
    exit 0;
fi
