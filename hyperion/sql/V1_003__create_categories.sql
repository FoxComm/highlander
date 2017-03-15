create table amazon_categories(
  id bigserial primary key,
  node_id int8 null,
  node_path varchar(255) null,
  size_opts varchar(255) null,
  department varchar(255) null,
  item_type varchar(255) null,
  inserted_at timestamp not null,
  updated_at timestamp not null
);
