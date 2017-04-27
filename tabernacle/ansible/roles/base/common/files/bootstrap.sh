#!/usr/bin/env bash
US=buildkite-agent
PUB_KEY="ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCf//As7IYiFXjmiFMcJ7m5oFs6+04nciqYe84DOrfxsnBHzc6bZ6JshI4Y8n63lzFEF0dsU/flK+OhGY/nGKCGw6QDi1dTTZdr4u+5BuEM7upQjG2SEg4UJpcLEsQSVFpRZhyGgM6ouQecEUFHz/YrGIb/cN8LdFLu8LWKZVyC2NhFvgndeYK+shitLUwfucSpnPVUqMRvzGZACcriXcnYUbE6StxM0gmdZxy/Hb2BU3TsZ1+UmSKLv+DZIyX9E7zAYrMIFvQVjG4GN1REvOqHnuLN0Ffcb3yDTUSoaKzNPyGEk38TStjzVDPKUKq2Ed1iP/+NRoJeTT2eo+ssHUbV buildkite-agent@stage-buildkite"

sudo useradd --create-home $US
sudo adduser $US admin

sudo cp /etc/sudoers /etc/sudoers.bk
sudo bash -c "echo '$US ALL=NOPASSWD: ALL' >> /etc/sudoers.bk"
sudo visudo -c -f /etc/sudoers.bk || { echo 'bad sudoers'; exit 1; }
sudo cp /etc/sudoers.bk /etc/sudoers

sudo chown -R $US:$US /home/$US
sudo rm -rf /home/$US/.ssh
sudo su $US -c "mkdir /home/$US/.ssh"
sudo su $US -c "echo '$PUB_KEY' >> /home/$US/.ssh/authorized_keys"
