#!/usr/bin/env bash
set -euo pipefail

curl -XGET http://{{mesos_ip}}:5050/version
