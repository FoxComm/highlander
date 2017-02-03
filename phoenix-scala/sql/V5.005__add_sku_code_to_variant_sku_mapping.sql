alter table product_variant_mwh_sku_ids rename to product_variant_skus;
alter table product_variant_skus rename column mwh_sku_id to sku_id;
alter table product_variant_skus add column sku_code sku_code;
