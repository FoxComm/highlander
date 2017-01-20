#!/usr/bin/env bash

IP=`hostname -I | awk '{print $1}'`
curl http://$IP:5050
