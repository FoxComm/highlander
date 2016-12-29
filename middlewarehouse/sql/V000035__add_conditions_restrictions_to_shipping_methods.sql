alter table shipping_methods add column created_at generic_timestamp_now;
alter table shipping_methods add column updated_at generic_timestamp_now;
alter table shipping_methods add column deleted_at generic_timestamp_null;
alter table shipping_methods add column conditions jsonb null;
alter table shipping_methods add column restrictions jsonb null;
