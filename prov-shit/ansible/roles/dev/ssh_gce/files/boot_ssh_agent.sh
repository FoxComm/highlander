#!/usr/bin/env bash

if [ -z "$SSH_AUTH_SOCK" ] ; then
  eval `ssh-agent -s`
  ssh-add .ssh/bk_rsa
fi
