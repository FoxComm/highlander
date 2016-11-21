alter table shipping_methods add column shipping_type integer not null;
alter table shipping_methods add column expected_cost integer not null;
alter table shipping_methods add column actual_cost integer null;
