#!/usr/bin/env bash

psql -U{{db_user}} -h{{consul_hosts.db}} -c'select public.update_customers_ranking()' {{db_name}}
