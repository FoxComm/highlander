create domain json_timestamp char(24) check (value ~* '\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z');
