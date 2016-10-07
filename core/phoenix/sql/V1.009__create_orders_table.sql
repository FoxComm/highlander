create table orders (
    id bigint primary key,
    reference_number reference_number not null unique default '',
    customer_id integer,
    context_id integer,
    state generic_string not null default 'remorseHold',
    placed_at generic_timestamp,
    updated_at generic_timestamp,
    remorse_period_end timestamp without time zone, -- managed by trigger
    currency currency,
    sub_total integer not null default 0,
    shipping_total integer not null default 0,
    adjustments_total integer not null default 0,
    taxes_total integer not null default 0,
    grand_total integer not null default 0,
    fraud_score integer not null default 0,
    constraint valid_state check (state in ('ordered','fraudHold','remorseHold','manualHold','canceled',
                                              'fulfillmentStarted','shipped')),
    foreign key (id) references cords(id),
    foreign key (reference_number) references cords(reference_number)
);

create index orders_customer_and_state_idx on orders (customer_id, state);

-- Delete cart and update cord when new order is created
create or replace function update_cord_for_order() returns trigger as $$
declare
    cord_id  integer;
begin
    update cords set is_cart = false where reference_number = new.reference_number;
    delete from carts where reference_number = new.reference_number;

    select id into cord_id from cords where reference_number = new.reference_number;
    new.id = cord_id;
    return new;
end;
$$ language plpgsql;

create trigger update_cord_for_order_trg
    before insert
    on orders
    for each row
    execute procedure update_cord_for_order();

-- Set remorse period end if new state is remorse_hold and no value is provided
-- Set remorse period end to null for any other order state
create function manage_remorse_period() returns trigger as $$
declare
begin
    if new.state = 'remorseHold' and new.remorse_period_end is null then
        new.remorse_period_end = now() + (30 ||' minutes')::interval;
    end if;
    if new.state != 'remorseHold' then
        new.remorse_period_end = null;
    end if;
    return new;
end;
$$ language plpgsql;

create trigger manage_remorse_period_update_trg
    before update of state
    on orders
    for each row
    execute procedure manage_remorse_period();

create trigger manage_remorse_period_create_trg
    before insert
    on orders
    for each row
    execute procedure manage_remorse_period();
