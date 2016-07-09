create table carts (
    id bigint primary key,
    reference_number reference_number not null unique,
    customer_id integer,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    currency currency,
    sub_total integer not null default 0,
    shipping_total integer not null default 0,
    adjustments_total integer not null default 0,
    taxes_total integer not null default 0,
    grand_total integer not null default 0,
    is_locked boolean default false,
    is_active boolean default true,
    foreign key (id) references inventory_events(id) on update restrict on delete restrict
);

create unique index customer_has_only_one_cart on carts (customer_id, is_active) where is_active = true;

-- need to run this after inventory_id_trigger, hence the z prefix
create function z_set_cart_reference_number() returns trigger as $$
declare
    reference_number reference_number default 0;
    prefix character(2) default 'BR';
    start_number integer default 10000;
begin
    if length(new.reference_number) = 0 then
        new.reference_number = concat(prefix, start_number + new.id);
    end if;
    return new;
end;
$$ language plpgsql;

create trigger set_inventory_id_trigger
    before insert
    on carts
    for each row
    execute procedure set_inventory_event_id();

-- generate a reference number and assign it to cart
create trigger z_set_cart_reference_number_trg
    before insert
    on carts
    for each row
    execute procedure z_set_cart_reference_number();
