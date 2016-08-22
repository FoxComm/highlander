alter domain json_timestamp drop constraint json_timestamp_check;
alter domain json_timestamp
        add constraint es_date_format_check
            check (value ~* '\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d{3})?Z');
