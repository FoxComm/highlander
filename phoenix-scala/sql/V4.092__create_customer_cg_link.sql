create table customer_group_members (
  id serial primary key,
  group_id integer references customer_dynamic_groups(id),
  customer_data_id integer references customer_data(id),
  created_at generic_timestamp
);