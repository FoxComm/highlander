#!/bin/bash
set -euo pipefail

./counter.p6 $KAFKA $TOPIC $HENHOUSE 2>&1 | tee /logs/digger-sphex.log
