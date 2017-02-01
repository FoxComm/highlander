#!/usr/bin/env bash

JOBS=5000
J=$1

SIMULATOR=$2
PERSONA=$3

seq -w 0 $JOBS | parallel -j$J --ungroup node --harmony src/simulate.js $SIMULATOR $PERSONA

