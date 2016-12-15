alter table skus rename to product_variants;
alter table variants rename to product_options;
alter table variant_variant_value_links rename to product_option_value_links;

alter table variant_value_sku_links rename to product_value_variant_links;


alter table product_variant_links rename to product_option_links;