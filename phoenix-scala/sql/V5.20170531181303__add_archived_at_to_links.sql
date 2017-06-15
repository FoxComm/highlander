create domain generic_timestamp_null timestamp without time zone null;

alter table album_image_links add column archived_at generic_timestamp_null;
alter table product_album_links add column archived_at generic_timestamp_null;
alter table product_sku_links add column archived_at generic_timestamp_null;
alter table product_variant_links add column archived_at generic_timestamp_null;
alter table promotion_discount_links add column archived_at generic_timestamp_null;
alter table sku_album_links add column archived_at generic_timestamp_null;
alter table variant_variant_value_links add column archived_at generic_timestamp_null;
alter table variant_value_sku_links add column archived_at generic_timestamp_null;
