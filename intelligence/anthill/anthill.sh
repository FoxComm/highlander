#!/bin/bash
set -euo pipefail

export LC_ALL=C.UTF-8
export LANG=C.UTF-8
python3 /anthill/src/router.py 2>&1 | tee /logs/anthill.log
