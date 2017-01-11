create table customer_groups_search_view (
  id integer primary key,
  name generic_string,
  customers_count integer null,
  client_state generic_string,
  elastic_request generic_string,
  updated_at json_timestamp,
  created_at json_timestamp
);
