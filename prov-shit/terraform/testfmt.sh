#!/usr/bin/env bash
if [[ $(terraform fmt) ]]; then
    echo "Unformatted Terraform files found";
    echo "Please run \`terraform fmt\`";
    exit 1;
fi
