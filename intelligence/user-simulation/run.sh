#!/usr/bin/env bash

PERSONA=McShopFace
JOBS=5000

seq -w 0 $JOBS | parallel --ungroup node --harmony src/simulate.js $PERSONA

