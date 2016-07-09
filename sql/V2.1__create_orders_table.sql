create table orders (
    id bigint primary key,
    reference_number reference_number not null unique,
    customer_id integer,
    context_id integer,
    state generic_string not null,
    placed_at generic_timestamp,
    updated_at generic_timestamp,
    remorse_period_end timestamp without time zone null,
    currency currency,
    sub_total integer not null default 0,
    shipping_total integer not null default 0,
    adjustments_total integer not null default 0,
    taxes_total integer not null default 0,
    grand_total integer not null default 0,
    fraud_score integer not null default 0,
    foreign key (id) references inventory_events(id) on update restrict on delete restrict,
    constraint valid_state check (state in ('ordered','fraudHold','remorseHold','manualHold','canceled',
                                              'fulfillmentStarted','shipped'))
);

create index orders_customer_and_state_idx on orders (customer_id, state);

create trigger set_inventory_id_trigger
    before insert
    on orders
    for each row
    execute procedure set_inventory_event_id();

-- Sets remorse period when order moves to remorseHold state
create function start_remorse_period() returns trigger as $$
declare
begin
  if old.state != 'remorseHold' and new.state = 'remorseHold' then
    new.remorse_period_end = now() + (30 ||' minutes')::interval;
  end if;
  return new;
end;
$$ language plpgsql;

create trigger remorse_hold_start before update of state on orders
for each row execute procedure start_remorse_period();

-- Reset remorse period when order moves from remorseHold state
create function reset_remorse_period() returns trigger as $$
declare
begin
  if old.state = 'remorseHold' and new.state != 'remorseHold' then
    new.remorse_period_end = null;
  end if;
  return new;
end;
$$ language plpgsql;

create trigger remorse_hold_watch before update of state on orders
for each row execute procedure reset_remorse_period();
