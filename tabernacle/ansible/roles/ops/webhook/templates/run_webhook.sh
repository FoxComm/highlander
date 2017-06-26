#!/usr/bin/env bash
set -euo pipefail

/usr/local/bin/webhook -port 80 \
    -hotreload \
    -verbose \
    -hooks {{webhook_dir}}/hooks.json
