#!/bin/bash

is_installed() {
  apt-cache policy $1 | grep Installed | grep -v none
}

get_version() {
  regex="[0-9]+\.[0-9]+\.[0-9]+"
  version=$(is_installed $1)

  if [[ $version =~ $regex ]]; then
    printf ${BASH_REMATCH[0]};
  fi
}
