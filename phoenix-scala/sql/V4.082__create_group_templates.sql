create table customer_group_templates (
  id serial primary key,
  name generic_string not null,
  client_state jsonb not null,
  elastic_request jsonb not null
);