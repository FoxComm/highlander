create or replace function truncate_nonempty_tables(text[]) returns int as $$
declare
  tables text[];
  t text;
  v boolean;
begin
  foreach t in array $1 loop
    execute format('select true from %s limit 1', t) into v;
    if v is true then
        tables := array_append(tables, t);
    end if;
  end loop;
  if array_length(tables, 1) > 0 then
    t := array_to_string(tables, ',');
    execute format('truncate %s restart identity cascade', t);
    return 0;
  end if;

  return 1;
end;
$$ language plpgsql;
