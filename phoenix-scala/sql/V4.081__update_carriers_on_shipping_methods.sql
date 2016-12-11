alter table shipping_methods drop column shipping_carrier_id;
alter table shipping_methods add column carrier generic_string;