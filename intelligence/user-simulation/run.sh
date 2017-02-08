#!/usr/bin/env bash

JOBS=5000
J=$1

HOST=$2
SIMULATOR=$3
PERSONA=$4

seq -w 0 $JOBS | parallel -j$J --ungroup node --harmony src/simulate.js $HOST $SIMULATOR $PERSONA

