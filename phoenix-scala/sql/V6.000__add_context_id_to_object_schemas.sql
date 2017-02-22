alter table object_schemas add column context_id integer references object_contexts(id) on update restrict on delete restrict;
alter table object_full_schemas add column context_id integer references object_contexts(id) on update restrict on delete restrict;

update object_schemas as schemas
  set context_id = context.context_id
  from (select id as context_id
        from object_contexts
        limit 1) as context
  where schemas.context_id is null;

update object_full_schemas as schemas
  set context_id = context.context_id
  from (select id as context_id
        from object_contexts
        limit 1) as context
  where schemas.context_id is null;

  alter table object_schemas alter column context_id set not null;
  alter table object_full_schemas alter column context_id set not null;
