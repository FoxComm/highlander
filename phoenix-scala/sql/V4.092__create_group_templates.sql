create table customer_group_templates (
  id serial primary key,
  name generic_string not null,
  client_state jsonb not null,
  elastic_request jsonb not null
);

create table group_template_instances (
  id serial primary key,
  group_template_id integer references customer_group_templates (id),
  group_id integer references customer_dynamic_groups (id),
  scope exts.ltree
);