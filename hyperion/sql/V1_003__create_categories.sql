CREATE TABLE amazon_categories(
  id bigserial PRIMARY KEY,
  node_id int8 NULL,
  node_path varchar(255) NULL,
  size_opts varchar(255) NULL,
  department varchar(255) NULL,
  item_type varchar(255) NULL,
  inserted_at timestamp NOT NULL,
  updated_at timestamp NOT NULL
);
