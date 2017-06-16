#!/usr/bin/env bash
set -euo pipefail

/usr/sbin/openvpn --writepid /run/openvpn/server.pid --daemon ovpn-server --cd /etc/openvpn --config /etc/openvpn/server.conf --script-security 2
