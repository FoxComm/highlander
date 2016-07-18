create table carts (
    id bigint primary key,
    reference_number reference_number not null unique default '',
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
    foreign key (id) references cords(id),
    foreign key (reference_number) references cords(reference_number)
);

create unique index customer_has_only_one_cart on carts (customer_id);

-- Create a cord when new cart is created
create or replace function create_cord_for_cart() returns trigger as $$
declare
    cord_id  integer;
    cord_ref reference_number;
begin
    with id_ref as (
      -- propagate cart's refnum, i.e. don't override one if provided
      insert into cords (reference_number) values (new.reference_number) returning *
    )
    select id, reference_number into cord_id, cord_ref from id_ref;
    new.id = cord_id;
    new.reference_number = cord_ref;
    return new;
end;
$$ language plpgsql;

create trigger create_cord_for_cart_trg
    before insert
    on carts
    for each row
    execute procedure create_cord_for_cart();
