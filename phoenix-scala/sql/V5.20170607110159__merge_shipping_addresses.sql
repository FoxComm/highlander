alter table addresses add column cord_ref text references cords (reference_number) on update restrict on delete restrict;
alter table addresses add column parent_id int references addresses (id) on delete restrict;

create index on addresses (cord_ref);
create index on addresses (parent_id);

alter table shipments drop constraint shipments_shipping_address_id_fkey;
alter table shipments add constraint shipments_shipping_address_id_fkey
  foreign key (shipping_address_id) references addresses (id)
  on update restrict on delete restrict;

-- move everything from order_shipping_addresses to addresses
insert into addresses(account_id, region_id, name, address1, address2, city, zip, phone_number, created_at, updated_at, cord_ref, parent_id)
  select
    coalesce(c.account_id, o.account_id),
    osa.region_id,
    osa.name,
    osa.address1,
    osa.address2,
    osa.city,
    osa.zip,
    osa.phone_number,
    osa.created_at,
    osa.updated_at,
    osa.cord_ref,
    parent_address.id
  from order_shipping_addresses as osa
    left outer join carts as c on osa.cord_ref = c.reference_number
    left outer join orders as o on osa.cord_ref = o.reference_number
    left outer join addresses as parent_address on parent_address.account_id = coalesce(c.account_id, o.account_id)
                                                   and parent_address.address1 = osa.address1 and parent_address.address2 = osa.address2
                                                   and parent_address.city = osa.city and parent_address.zip = osa.zip
                                                   and parent_address.parent_id is null;


-- is moved to R__orders_search_view_triggers.sql
drop function if exists update_orders_view_from_shipping_addresses_fn() cascade;
drop table order_shipping_addresses;

