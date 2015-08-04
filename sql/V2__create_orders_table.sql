create table orders (
    id bigint primary key,
    reference_number character varying(20) not null,
    customer_id integer,
    status character varying(255) not null,
    locked boolean default false,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    placed_at timestamp without time zone null,
    remorse_period int default 30,
    foreign key (id) references inventory_events(id) on update restrict on delete restrict,
    constraint valid_reference_number check (length(reference_number) > 0),
    constraint valid_status check (status in ('cart','ordered','fraudHold','remorseHold','manualHold','canceled',
                                              'fulfillmentStarted','shipped'))
);

create index orders_customer_and_status_idx on orders (customer_id, status);

-- partial index ensures we never have more than 1 cart per customer
create unique index orders_has_only_one_cart on orders (customer_id, status)
    where status = 'cart';

create function set_order_reference_number() returns trigger as $$
declare
    reference_number character varying(255) default 0;
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
    on orders
    for each row
    execute procedure set_inventory_event_id();

-- generate a reference number and assign it to order
create trigger set_order_reference_number_trg
    before insert
    on orders
    for each row
    execute procedure set_order_reference_number();

