do $$
begin
if exists (select * from pg_available_extensions where name = 'temporal_tables') then
  create extension if not exists temporal_tables with schema public;
end if;
end
$$