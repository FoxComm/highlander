#!/usr/bin/env bash

set -ue

# Add Buildkite signed apt repo
sudo sh -c 'echo deb https://apt.buildkite.com/buildkite-agent stable main > /etc/apt/sources.list.d/buildkite-agent.list'
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys 32A37959C2FA5C3C99EFBC32A79206696452D198

# Install agent
sudo apt-get update && sudo apt-get install -y buildkite-agent

# Configure agent token
sudo sed -i "s/xxx/2d1e50045123e180d8b12c3b46c8a8dff29e38aaa4c486824d/g" /etc/buildkite-agent/buildkite-agent.cfg

# Set agent queue
sudo bash -c 'echo "meta-data=\"queue=core\"" >> /etc/buildkite-agent/buildkite-agent.cfg'

# Start agent
sudo systemctl enable buildkite-agent && sudo systemctl start buildkite-agent

# Setup Access from ansible provisioning machine
US=buildkite-agent
PUB_KEY="ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCf//As7IYiFXjmiFMcJ7m5oFs6+04nciqYe84DOrfxsnBHzc6bZ6JshI4Y8n63lzFEF0dsU/flK+OhGY/nGKCGw6QDi1dTTZdr4u+5BuEM7upQjG2SEg4UJpcLEsQSVFpRZhyGgM6ouQecEUFHz/YrGIb/cN8LdFLu8LWKZVyC2NhFvgndeYK+shitLUwfucSpnPVUqMRvzGZACcriXcnYUbE6StxM0gmdZxy/Hb2BU3TsZ1+UmSKLv+DZIyX9E7zAYrMIFvQVjG4GN1REvOqHnuLN0Ffcb3yDTUSoaKzNPyGEk38TStjzVDPKUKq2Ed1iP/+NRoJeTT2eo+ssHUbV buildkite-agent@stage-buildkite"

sudo cp /etc/sudoers /etc/sudoers.bk
sudo bash -c "echo '$US ALL=NOPASSWD: ALL' >> /etc/sudoers.bk"
sudo visudo -c -f /etc/sudoers.bk || { echo 'bad sudoers'; exit 1; }
sudo cp /etc/sudoers.bk /etc/sudoers

sudo su $US -c "mkdir -p /var/lib/buildkite-agent/.ssh"
sudo su $US -c "echo '$PUB_KEY' >> /var/lib/buildkite-agent/.ssh/authorized_keys"

# Change home directory ownership
sudo mkdir -p /home/buildkite-agent
sudo chown buildkite-agent /home/buildkite-agent
