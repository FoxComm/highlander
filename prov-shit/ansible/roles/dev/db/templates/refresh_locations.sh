#!/bin/bash

psql -U{{db_user}} -h{{db_host}} -c'update countries set is_billable = NOT is_billable where 1 = 1' {{db_name}}
