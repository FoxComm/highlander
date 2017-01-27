----- update view
alter table product_variants_search_view add column middlewarehouse_sku_id int;

drop index if exists sku_search_view_idx;
alter table product_variants_search_view
  drop constraint if exists sku_search_view_pkey,
  drop constraint if exists sku_search_view_id;

update product_variants_search_view
set id = product_variants.form_id
from product_variants
where product_variants.id = product_variants_search_view.id;

alter table product_variants_search_view
  add constraint product_variants_search_view_pkey primary key (id, context_id),
  add foreign key (id) references object_forms (id) on delete restrict on update cascade,
  add foreign key (context_id) references object_contexts (id) on delete restrict on update cascade,
  add foreign key (middlewarehouse_sku_id) references product_variant_mwh_sku_ids (mwh_sku_id) on delete restrict on update cascade;

----- we drop old triggers here, but create new renamed one
----- in repeatable migration file: R__product_variants_search_view_triggers.sql
drop trigger if exists insert_skus_view_from_skus on product_variants;
drop function if exists insert_skus_view_from_skus_fn();

drop trigger if exists update_skus_view_from_object_forms on object_contexts;
drop function if exists update_skus_view_from_object_context_fn();

drop trigger if exists update_skus_view_from_object_head_and_shadows on product_variants;
drop function if exists update_skus_view_from_object_attrs_fn();

drop trigger if exists update_skus_view_image on product_album_links_view;
drop trigger if exists update_skus_view_image on product_to_variant_links;
drop function if exists update_skus_view_image_fn();
