----- update view
alter table product_variants_search_view add column mwh_sku_id int;

alter table product_variants_search_view
  drop constraint sku_search_view_pkey,
  add constraint product_variants_search_view_pkey primary key (id, context_id);

update product_variants_search_view
set id = product_variants.form_id
from product_variants
where product_variants.id = product_variants_search_view.id;

----- triggers are updated in repeatable migration file: R__product_variants_search_view_triggers.sql
