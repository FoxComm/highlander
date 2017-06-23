create table address_cord(
  id serial primary key,
  cord_ref text not null references cords(reference_number) on delete restrict,
  address_id int not null references addresses(id) on delete restrict
);

create index on address_cord (cord_ref);

-- populate mapping table with data from order_shipping_addresses
insert into address_cord (cord_ref, address_id)
  select
    osa.cord_ref,
    a.id
  from order_shipping_addresses as osa
    left outer join carts as c on osa.cord_ref = c.reference_number
    left outer join orders as o on osa.cord_ref = o.reference_number
    inner join addresses as a on a.account_id = coalesce(c.account_id, o.account_id)
                                 and a.address1 = osa.address1 and a.address2 = osa.address2
                                 and a.city = osa.city and a.zip = osa.zip;

-- enforce address fields uniqueness to eliminate duplicates
alter table addresses add constraint address_fields_unique unique (account_id, address1, address2, city, zip);

alter table shipments drop constraint shipments_shipping_address_id_fkey;
alter table shipments add constraint shipments_shipping_address_id_fkey
  foreign key (shipping_address_id) references addresses (id)
  on update restrict on delete restrict;

-- is moved to R__orders_search_view_triggers.sql
drop function if exists update_orders_view_from_shipping_addresses_fn() cascade;
drop table order_shipping_addresses;

