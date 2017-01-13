create table product_variant_mwh_sku_ids (
  id serial primary key,
  variant_form_id int not null references object_forms(id) on update restrict on delete restrict,
  mwh_sku_id int not null,
  created_at generic_timestamp
);

create unique index mwh_sku_id_variant_form_id_idx on product_variant_mwh_sku_ids(variant_form_id);
create unique index mwh_sku_id_idx on product_variant_mwh_sku_ids(mwh_sku_id);
