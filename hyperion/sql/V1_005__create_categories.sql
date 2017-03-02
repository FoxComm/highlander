CREATE TABLE public.amazon_categories (
  id int4 NOT NULL DEFAULT nextval('amazon_categories_id_seq'::regclass),
  node_id int8 NULL,
  node_path varchar(255) NULL,
  size_opts varchar(255) NULL,
  department varchar(255) NULL,
  item_type varchar(255) NULL,
  inserted_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  CONSTRAINT amazon_categories_pkey PRIMARY KEY (id)
)
WITH (
  OIDS=FALSE
) ;
