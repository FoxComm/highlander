#!/usr/bin/env bash
while :
do
    {{update_materialized_views_cmd}} 2>&1 > /dev/null
    sleep 1
done
