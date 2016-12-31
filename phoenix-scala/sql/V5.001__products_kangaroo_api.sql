

-- Update kinds in object forms
update object_forms set kind = 'product-option' where kind = 'variant';
update object_forms set kind = 'product-option-value' where kind = 'variant-value';
update object_forms set kind = 'product-variant' where kind = 'sku';



update object_shadows set json_schema = 'product-variant' where json_schema = 'sku';

create or replace function update_object_schemas_insert_fn() returns trigger as $$
declare
  dep text;
  did int;
begin
  -- check deps are already exists in object_schemas
    foreach dep in array new.dependencies
      loop
        select id into did from object_schemas where "name" = dep;
         if did is null then
            raise exception 'ObjectSchema with name % doesn''t exist', dep;
         end if;
      end loop;
  --
  insert into object_full_schemas(id, kind, "name", "schema", created_at)
    values (new.id, new.kind, new.name, new.schema || get_definitions_for_object_schema(new.name), new.created_at);

  return null;
end;
$$ language plpgsql;

create or replace function update_object_schemas_update_fn() returns trigger as $$
declare
  dep text;
  did int;
begin
  -- check deps are already exists in object_schemas
    foreach dep in array new.dependencies
      loop
        select id into did from object_schemas where "name" = dep;
         if did is null then
            raise exception 'ObjectSchema with name % doesn''t exist', dep;
         end if;
      end loop;
  --
  update object_full_schemas
    set schema = new.schema ||get_definitions_for_object_schema(new.name),
      kind = new.kind,
      "name" = new.name;

  return null;
end;
$$ language plpgsql;

create trigger update_object_schemas_update
    after update on object_schemas
    for each row
    execute procedure update_object_schemas_update_fn();

drop trigger if exists update_object_schemas_insert on object_schemas;

create trigger update_object_schemas_insert
    after insert on object_schemas
    for each row
    execute procedure update_object_schemas_insert_fn();

update object_schemas set name = 'product-variant' where name = 'sku';
update object_schemas set kind = 'product-variant' where kind = 'sku';

-- notes

update notes set reference_type = 'product-variant' where reference_type = 'sku';

-- will trigger reindexing, it's ok.
update notes_search_view set reference_type = 'product-variant' where reference_type = 'sku';

