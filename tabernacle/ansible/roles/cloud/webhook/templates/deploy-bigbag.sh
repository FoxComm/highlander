#!/usr/bin/env bash

set -ue

# Clone and build
git clone https://github.com/kpashka/bigbag.git
cd bigbag
make docker

# Trigger Marathon deployment

# Cleanup
cd ../
rm -rf bigbag
