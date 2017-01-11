alter table customer_dynamic_groups add column scope exts.ltree not null;

create table customer_groups_search_view (
  id bigint primary key,
  group_id bigint not null,
  name generic_string,
  customers_count integer null,
  client_state generic_string,
  elastic_request generic_string,
  updated_at json_timestamp,
  created_at json_timestamp,
  scope exts.ltree
);
