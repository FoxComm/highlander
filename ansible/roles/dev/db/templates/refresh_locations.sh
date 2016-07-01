#!/bin/bash

psql -U{{db_user}} -h{{db_host}} -c'update countries set name = name where 1 = 1' {{db_name}}
psql -U{{db_user}} -h{{db_host}} -c'update regions set name = name where 1 = 1' {{db_name}}
