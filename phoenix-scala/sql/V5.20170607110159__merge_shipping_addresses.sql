alter table addresses add column cord_ref text constraint order_shipping_addresses_cord_ref_fkey
references cords (reference_number) on update restrict on delete restrict;

