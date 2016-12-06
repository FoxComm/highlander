create or replace function filter_empty_tables(text[]) returns text[] as $$
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

  return tables;
end;
$$ language plpgsql;