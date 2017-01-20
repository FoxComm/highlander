#!/usr/bin/env bash

psql -U{{db_user}} -h{{db_host}} -c'select public.update_customers_ranking()' {{db_name}}
