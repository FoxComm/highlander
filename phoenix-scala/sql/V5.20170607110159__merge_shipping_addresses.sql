alter table addresses add column cord_ref text constraint order_shipping_addresses_cord_ref_fkey
references cords (reference_number) on update restrict on delete restrict;

create index addresses_cord_ref_id_idx
  on addresses (cord_ref);

alter table shipments drop constraint shipments_shipping_address_id_fkey;
alter table shipments add constraint shipments_shipping_address_id_fkey
  foreign key (shipping_address_id) references addresses (id)
  on update restrict on delete restrict;

-- move everything from order_shipping_addresses to addresses
create or replace function shipping_addresses_migration() returns text as $$
begin
    -- test data for migration
    insert into carts (account_id, reference_number, created_at, updated_at, currency, scope) values (1, 'TEST-ABC-1', now(), null, 'USD', '1');
    insert into order_shipping_addresses(cord_ref, region_id, name, address1, address2, city, zip, phone_number, created_at, updated_at)
    values ('TEST-ABC-1', 1, 'Test address', 'Test Case rd.', null, 'Testburg', '125438', '12345678', now(), null);

    -- data migration
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

    assert ((select a.cord_ref from addresses as a) = 'TEST-ABC-1'), 'Something is wrong with migration, `TEST-ABC-1` cord_ref was not found';

    -- delete test data
    delete from addresses where cord_ref = 'TEST-ABC-1';
    delete from order_shipping_addresses where cord_ref = 'TEST-ABC-1';
    delete from carts where reference_number = 'TEST-ABC-1';
    delete from cords where reference_number = 'TEST-ABC-1';

    return '';
  end;
  $$ language plpgsql;


select shipping_addresses_migration();

-- is moved to R__orders_search_view_triggers.sql
drop function if exists update_orders_view_from_shipping_addresses_fn() cascade;
drop table order_shipping_addresses;
