create table object_schema(
  id bigserial primary key,
  schema_name varchar(255) not null,
  schema jsonb not null,
  category_id int8,
  inserted_at timestamp not null,
  updated_at timestamp not null
);

alter table amazon_categories add column object_schema_id int8;
alter table object_schema add constraint schema_name_uniq unique (schema_name);