create table object_attributes_es_mapping(
  id serial primary key,
  es_index generic_string not null unique,
  schema_name generic_string not null references object_schemas(name) on update restrict on delete restrict,
  es_attributes jsonb -- TODO: add constraints ?
  -- es_attributes format
  -- [{path: {}, es_opts: {} }, ...]
);

create index object_attributes_es_mapping_schema_idx on object_attributes_es_mapping(schema_name);

create table object_schemas_es_view(
  id integer primary key,
  es_index generic_string not null,
  schema_name generic_string not null,
  schema_attributes jsonb,
  es_attributes jsonb,
  scopes jsonb,
  created_at json_timestamp
);

create or replace function update_object_schemas_es_insert_fn() returns trigger as $$
begin
  insert into object_schemas_es_view
    select distinct on (emap.id)
      emap.id,
      emap.es_index,
      o.name,
      (o.schema #>'{properties,attributes}')::jsonb,
      emap.es_attributes,
      jsonb_agg(get_scope_path(s.id)) over (partition by emap.id),
      to_json_timestamp(o.created_at)
    from object_attributes_es_mapping as emap
        inner join object_schemas as o on (emap.schema_name = o.name),
      scopes as s
      where emap.id = new.id;

  return null;
end;
$$ language plpgsql;

create or replace function update_object_schemas_es_from_schemas_fn() returns trigger as $$
begin
  update object_schemas_es_view
    set
      schema_attributes = (new.schema #>'{properties,attributes}')::jsonb
    where schema_name = new.name;
  return null;
end;
$$ language plpgsql;

create or replace function update_object_schemas_es_update_fn() returns trigger as $$
begin
  update object_schemas_es_view
    set
      es_index = new.es_index,
      es_attributes = new.es_attributes
    where id = new.id;
  return null;
end;
$$ language plpgsql;

create or replace function update_object_schemas_es_on_scopes_fn() returns trigger as $$
begin
  update object_schemas_es_view
    set scopes = q.scopes
    from (select distinct on (o.name)
        o.name,
        jsonb_agg(get_scope_path(s.id)) over (partition by o.id) as scopes
        from object_schemas as o,
          scopes as s) as q
      where object_schemas_es_view.schema_name = q.name;
  return null;
end;
$$ language plpgsql;

-- triggers

create trigger update_object_schemas_es_insert
    after insert on object_attributes_es_mapping
    for each row
    execute procedure update_object_schemas_es_insert_fn();

create trigger update_object_schemas_es_from_schemas
    after update on object_schemas
    for each row
    execute procedure update_object_schemas_es_from_schemas_fn();

create trigger update_object_schemas_es_update
    after update on object_attributes_es_mapping
    for each row
    execute procedure update_object_schemas_es_update_fn();

create trigger update_object_schemas_es_on_scopes
    after update or insert or delete on scopes
    for each row
    execute procedure update_object_schemas_es_on_scopes_fn();


-- fill object_schemas_scopes_insert
insert into object_schemas_es_view
  select distinct on (emap.id)
      emap.id,
      emap.es_index,
      o.name,
      (o.schema #>'{properties,attributes}')::jsonb as attrs,
      emap.es_attributes,
      jsonb_agg(get_scope_path(s.id)) over (partition by emap.id) as scopes,
      to_json_timestamp(o.created_at)
    from object_attributes_es_mapping as emap
        inner join object_schemas as o on (emap.schema_name = o.name),
      scopes as s;
