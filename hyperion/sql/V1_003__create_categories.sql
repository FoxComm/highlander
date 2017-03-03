CREATE SEQUENCE amazon_categories_id_seq;
CREATE TABLE amazon_categories(
  id INT NOT NULL DEFAULT nextval('amazon_categories_id_seq'),
  node_id int8 NULL,
  node_path varchar(255) NULL,
  size_opts varchar(255) NULL,
  department varchar(255) NULL,
  item_type varchar(255) NULL,
  inserted_at timestamp NOT NULL,
  updated_at timestamp NOT NULL
);
