alter table stock_locations drop column address;
alter table stock_locations add column address_id integer null references addresses(id) on update restrict on delete restrict;
