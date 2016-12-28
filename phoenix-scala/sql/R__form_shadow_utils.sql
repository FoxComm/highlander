-- Let this be here since function signature will change
drop function if exists illuminate_object(int);

-- Record for storing attr name and hash resolved from shadow JSON
do $$
begin
  if not exists (select 1 from pg_type where typname = 'shadow_attrs') then
    create type shadow_attrs as (attr text, hash text);
  end if;
end$$;

-- Usage: select * from illuminate_object(form_id);
create or replace function illuminate_object(fi int) returns table(k text, v text) as $$
declare
  shadow jsonb;
  form object_forms%rowtype;
  shadow_attr shadow_attrs%rowtype;
begin
  select * into form from object_forms where id=fi;
  -- first result row: kind of object
  return query select 'kind'::text as k, form.kind::text as v;

  -- TODO: handle multiple shadows
  select attributes into shadow from object_shadows where form_id=fi;

  -- loop through shadow attributes
  for shadow_attr in
    -- resolve keys and hashes
    select key as attr, value->>'ref' as hash from jsonb_each(shadow)
  loop
    -- append a row to result with key and resolved value from form
    return query select shadow_attr.attr as k, form.attributes->>shadow_attr.hash as v;
  end loop;

  -- return all results of "return query" statements as a table
  return;
end;
$$ language plpgsql;
