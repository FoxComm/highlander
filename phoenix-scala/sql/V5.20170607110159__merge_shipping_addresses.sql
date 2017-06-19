alter table addresses add column cord_ref text constraint order_shipping_addresses_cord_ref_fkey
references cords (reference_number) on update restrict on delete restrict;

create index addresses_cord_ref_id_idx
  on addresses (cord_ref);

alter table shipments drop constraint shipments_shipping_address_id_fkey;
alter table shipments add constraint shipments_shipping_address_id_fkey
  foreign key (shipping_address_id) references addresses (id)
  on update restrict on delete restrict;

-- move everything from order_shipping_addresses to addresses
insert into addresses(account_id, region_id, name, address1, address2, city, zip, phone_number, created_at, updated_at, deleted_at, cord_ref)
  select
    c.account_id,
    osa.region_id,
    osa.name,
    osa.address1,
    osa.address2,
    osa.city,
    osa.zip,
    osa.phone_number,
    osa.created_at,
    osa.updated_at,
    null, -- no deleted_at
    osa.cord_ref
  from order_shipping_addresses as osa
    inner join carts as c on osa.cord_ref = c.reference_number;

-- is moved to R__orders_search_view_triggers.sql
drop function if exists update_orders_view_from_shipping_addresses_fn() cascade;
drop table order_shipping_addresses;

