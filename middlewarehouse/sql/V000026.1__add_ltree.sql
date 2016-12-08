-- extensions
create schema if not exists exts;
create extension if not exists ltree schema exts;

do $$
begin
    execute 'alter database ' || current_database() || ' set search_path = "$user",public,exts';
end;
$$;
