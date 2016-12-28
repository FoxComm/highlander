alter table shipping_methods rename storefront_display_name to name;
alter table shipping_methods drop column admin_display_name;

alter table export_shipments drop column admin_display_name;
alter table export_shipments rename storefront_display_name to name;
