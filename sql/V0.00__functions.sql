-- extensions
create schema if not exists exts;
create extension if not exists pg_trgm schema exts;
create extension if not exists ltree schema exts;

DO $$
BEGIN
   execute 'alter database ' || current_database() || ' set search_path = "$user",public,exts';
END;
$$;

-- creates an inventory_event so we have an ID for the child table in our concrete table pattern
create function make_inventory_event_id() returns bigint as $$
declare
    event_id bigint;
begin
    insert into inventory_events default values returning id INTO event_id;
    return event_id;
end;
$$ language plpgsql;

create function set_inventory_event_id() returns trigger as $$
begin
    new.id = make_inventory_event_id();
    return new;
end;
$$ language plpgsql;
