#!/usr/bin/env bash
set -euo pipefail

/usr/local/bin/pgweb_linux_amd64 --bind={{pgweb_bind}} --listen={{pgweb_port}}
