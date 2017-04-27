#!/bin/bash

./counter.p6 $KAFKA $TOPIC $HENHOUSE 2>&1 | tee /logs/digger-sphex.log
