alter table album_image_links add column archived_at generic_timestamp;
alter table product_album_links add column archived_at generic_timestamp;
alter table product_sku_links add column archived_at generic_timestamp;
alter table product_variant_links add column archived_at generic_timestamp;
alter table promotion_discount_links add column archived_at generic_timestamp;
alter table sku_album_links add column archived_at generic_timestamp;
alter table variant_variant_value_links add column archived_at generic_timestamp;
alter table variant_value_sku_links add column archived_at generic_timestamp;

