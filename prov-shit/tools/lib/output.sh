#!/bin/bash

section() {
    local hr=$(printf '=%.s' $(seq 1 $(tput cols)))
    printf $hr
    printf "$1\n"
    printf $hr
}
