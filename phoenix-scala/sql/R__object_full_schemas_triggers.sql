create or replace function get_definitions_for_object_schema(text) returns jsonb as $$
declare result jsonb;
begin
  with recursive s_dependencies(s_name, s_schema, s_dep, agg) as (
  select "name", "schema", dependencies, 1
    from object_schemas
    where "name" = $1
  union all
    select s.name, s.schema, s.dependencies, 1
    from object_schemas s, s_dependencies d
    where s.name = ANY(d.s_dep)
)
  select
    json_build_object('definitions', jsonb_object_agg(s_name, s_schema)) into result
  from (select * from s_dependencies offset 1) as t -- skip target schema
  group by agg;

  return coalesce(result, '{"definitions": {}}'::jsonb);
end;
$$ language plpgsql;

-- insert & update
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
  insert into object_full_schemas(id, context_id, kind, "name", "schema", created_at)
    values(new.id, new.context_id, new.kind, new.name, new.schema || get_definitions_for_object_schema(new.name), new.created_at)
  on conflict ("name") do update
    set "schema" = new.schema || get_definitions_for_object_schema(new.name);

  return null;
end;
$$ language plpgsql;

drop trigger if exists update_object_schemas_insert on object_schemas;
create trigger update_object_schemas_insert
    after insert or update on object_schemas
    for each row
    execute procedure update_object_schemas_insert_fn();

-- delete
create or replace function delete_on_object_schemas_fn() returns trigger as $$
begin
  delete from object_full_schemas where id = old.id;
return null;
end;
$$ language plpgsql;

drop trigger if exists delete_on_object_schemas_insert on object_schemas;
create trigger delete_on_object_schemas_insert
  after delete on object_schemas
  for each row
  execute procedure delete_on_object_schemas_fn();

