--- Rename tables
alter table skus rename to product_variants;
alter table variants rename to product_options;
alter table variant_variant_value_links rename to product_option__value_links;
alter table variant_value_sku_links rename to product_value__variant_links;
alter table product_variant_links rename to product__option_links;
alter table product_sku_links rename to product__variant_links;
alter table sku_album_links rename to variant_album_links;

-- Update kinds in object forms
update object_forms set kind = 'product-option' where kind = 'variant';
update object_forms set kind = 'product-value' where kind = 'variant-value';
update object_forms set kind = 'product-variant' where kind = 'sku';

-- Drop sku_search_view
-- drop trigger insert_skus_view_from_skus on product_variants;
-- drop trigger update_skus_view_from_object_forms on object_contexts;
-- drop trigger update_skus_view_from_object_head_and_shadows on product_variants;

drop function update_skus_view_from_object_context_fn() cascade;
drop function insert_skus_view_from_skus_fn() cascade;
drop function update_skus_view_image_fn() cascade;

-- Views
alter table product_sku_links_view rename to product__variant_links_view;
alter table product__variant_links_view rename column skus to variants;