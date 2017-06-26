#!/usr/bin/env bash
set -euo pipefail

{{prometheus_location}}/prometheus -config.file=prometheus.yml
