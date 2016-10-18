create or replace function to_json_timestamp(timestamp) returns char(24) as $$
begin
    return to_char($1, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"');
end;
$$ language plpgsql;
