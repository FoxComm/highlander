#!/bin/bash

section() {
  local hr=$(printf '=%.s' $(seq 1 $(tput cols)))
  printf $hr
  printf "$1\n"
  printf $hr
}

red() {
  color '\e[31m' $1
}

green() {
  color '\e[32m' $1 
}

yellow() {
  color '\e[33m' $1 
}

blue() {
  color '\e[34m' $1 
}

color() {
  printf $1
  printf $2
  printf '\e[0m'
}
