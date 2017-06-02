#!/usr/bin/env bash

WRK_PARAMS=("-c32" "-c64" "-c92" "-c128" "-c160" "-c192")

# warmup for JIT-based services (e.g. all running on JVM)
# as vast majority of time when running in production
# JVM service should be already optimized by JIT
echo "Warming up service"
wrk -c192 -t4 -d20s -sall_doc.lua $1 > /dev/null 2>&1

echo "Benchmarking fetching of the entire document's source"
for p in "${WRK_PARAMS[@]}"
do
  wrk ${p} -t4 -d20s -sall_doc.lua $1
done
