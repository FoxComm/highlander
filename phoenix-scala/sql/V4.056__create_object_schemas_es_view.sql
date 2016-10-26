create table object_schemas_es_view(
  id integer primary key,
  kind generic_string not null,
  name generic_string not null unique,
  dependencies jsonb,
  "schema" jsonb,
  scopes jsonb,
  created_at json_timestamp
);

create or replace function update_object_schemas_es_insert_fn() returns trigger as $$
begin
  insert into object_schemas_es_view
    select distinct on (o.id)
      o.id,
      o.kind,
      o.name,
      array_to_json(dependencies)::jsonb,
      o.schema,
      jsonb_agg(get_scope_path(s.id)) over (partition by o.id),
      to_json_timestamp(o.created_at)
    from object_schemas as o,
      scopes as s
      where o.id = new.id;
  return null;
end;
$$ language plpgsql;

create or replace function update_object_schemas_es_update_fn() returns trigger as $$
begin
  update object_schemas_es_view
    set
      "name" = new.name,
      kind = new.kind,
      "schema" = new.schema,
      dependencies = array_to_json(new.dependencies)::jsonb
    where id = new.id;
  return null;
end;
$$ language plpgsql;

create or replace function update_object_schemas_es_on_scopes_fn() returns trigger as $$
begin
  update object_schemas_es_view
    set scopes = q.scopes
    from (select distinct on (o.id)
        o.id,
        jsonb_agg(get_scope_path(s.id)) over (partition by o.id) as scopes
        from object_schemas as o,
          scopes as s) as q
      where object_schemas_es_view.id = q.id;
  return null;
end;
$$ language plpgsql;

-- triggers

create trigger update_object_schemas_es_insert
    after insert on object_schemas
    for each row
    execute procedure update_object_schemas_es_insert_fn();

create trigger update_object_schemas_es_update
    after update on object_schemas
    for each row
    execute procedure update_object_schemas_es_update_fn();

create trigger update_object_schemas_es_on_scopes
    after update or insert or delete on scopes
    for each row
    execute procedure update_object_schemas_es_on_scopes_fn();


-- fill object_schemas_scopes_insert
insert into object_schemas_es_view
    select distinct on (o.id)
      o.id,
      o.kind,
      o.name,
      array_to_json(dependencies)::jsonb,
      o.schema,
      jsonb_agg(get_scope_path(s.id)) over (partition by o.id),
      to_json_timestamp(o.created_at)
    from object_schemas as o,
      scopes as s;
