#!/usr/bin/env bash
set -euo pipefail

/usr/local/bin/mesos-consul --refresh=5s \
    --healthcheck \
    --healthcheck-ip=127.0.0.1 \
    --healthcheck-port=24476 \
    --log-level=WARN
