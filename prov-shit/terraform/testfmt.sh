#!/usr/bin/env bash
set -ue

unformatted_files=$(terraform fmt)

if [[ $? != 0 ]]; then
    echo "`terraform fmt` command failed."
    exit 1;
else if [[ $unformatted_files ]]; then
    echo "Unformatted Terraform files found.";
    echo "Please run \`terraform fmt\`";
    exit 1;
fi
