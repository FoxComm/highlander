alter table addresses add column cord_ref text constraint order_shipping_addresses_cord_ref_fkey
references cords (reference_number) on update restrict on delete restrict;

alter table shipments drop constraint shipments_shipping_address_id_fkey;
alter table shipments add constraint shipments_shipping_address_id_fkey
  foreign key (shipping_address_id) references addresses (id)
  on update restrict on delete restrict;


-- todo move everything from order_shipping_addresses to addresses
--

-- todo fix all deps

-- todo drop order_shipping_addresses and dependencies
-- drop table order_shipping_addresses;