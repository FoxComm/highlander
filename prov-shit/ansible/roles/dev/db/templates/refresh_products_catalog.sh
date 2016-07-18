#!/bin/bash

psql -U{{db_user}} -h{{db_host}} -c'select public.toggle_products_catalog_from_to_active()' {{db_name}}
