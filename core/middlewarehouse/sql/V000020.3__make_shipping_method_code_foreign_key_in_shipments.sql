alter table shipments drop column shipping_method_id;
alter table shipments add column shipping_method_code generic_string not null;
alter table shipments add foreign key (shipping_method_code) references shipping_methods(code) on update restrict on delete restrict;
