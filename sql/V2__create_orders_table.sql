create table orders (
    id bigint primary key,
    reference_number reference_number not null unique,
    customer_id integer,
    status generic_string not null,
    locked boolean default false,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    placed_at timestamp without time zone null,
    remorse_period_end timestamp without time zone null,
    rma_count integer default 0,
    currency currency,
    foreign key (id) references inventory_events(id) on update restrict on delete restrict,
    constraint valid_status check (status in ('cart','ordered','fraudHold','remorseHold','manualHold','canceled',
                                              'fulfillmentStarted','shipped'))
);

create index orders_customer_and_status_idx on orders (customer_id, status);

-- partial index ensures we never have more than 1 cart per customer
create unique index orders_has_only_one_cart on orders (customer_id, status)
    where status = 'cart';

-- atomic increment procedure, used for sequential RMA suffix generation
create function next_rma_id(order_id integer) returns integer as $$
    update orders set rma_count = rma_count + 1 where id=$1 returning rma_count;
$$ language 'sql';

create function set_order_reference_number() returns trigger as $$
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
    on orders
    for each row
    execute procedure set_inventory_event_id();

-- generate a reference number and assign it to order
create trigger set_order_reference_number_trg
    before insert
    on orders
    for each row
    execute procedure set_order_reference_number();

-- Sets remorse period when order moves to remorseHold status
create function start_remorse_period() returns trigger as $$
declare
begin
  if old.status != 'remorseHold' and new.status = 'remorseHold' then
    new.remorse_period_end = now() + (30 ||' minutes')::interval;
  end if;
  return new;
end;
$$ language plpgsql;

create trigger remorse_hold_start before update of status on orders
for each row execute procedure start_remorse_period();

-- Reset remorse period when order moves from remorseHold status
create function reset_remorse_period() returns trigger as $$
declare
begin
  if old.status = 'remorseHold' and new.status != 'remorseHold' then
    new.remorse_period_end = null;
  end if;
  return new;
end;
$$ language plpgsql;

create trigger remorse_hold_watch before update of status on orders
for each row execute procedure reset_remorse_period();
