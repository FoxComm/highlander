alter table shipping_methods add column external_freight_id integer null references external_freights(id) on update restrict on delete restrict;
