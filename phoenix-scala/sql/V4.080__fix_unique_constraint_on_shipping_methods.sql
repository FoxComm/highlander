alter table shipping_methods drop constraint shipping_methods_code_key;
create unique index shipping_methods_code_idx on shipping_methods (lower(code)) where is_active = true;
