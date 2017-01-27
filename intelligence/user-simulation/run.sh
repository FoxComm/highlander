#!/usr/bin/env bash

PERSONA=McShopFace
JOBS=5000
J=$1

seq -w 0 $JOBS | parallel -j$J --ungroup node --harmony src/simulate.js $PERSONA

