create table catalog_products(
  id serial primary key,
  catalog_id integer not null references catalogs(id) on update restrict on delete restrict,
  product_id integer not null references object_forms(id) on update restrict on delete restrict,
  created_at generic_timestamp not null,
  archived_at generic_timestamp_null
);

create index catalogs_id_idx on catalog_products (catalog_id);
create index products_id_idx on catalog_products (product_id);
