create table product_variant_sku_ids (
  id serial primary key,
  variant_form_id int not null references object_forms(id) on update restrict on delete restrict unique,
  mwh_sku_id int not null,
  created_at generic_timestamp
);
