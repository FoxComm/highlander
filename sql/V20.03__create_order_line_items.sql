create table order_line_items (
    id serial primary key,
    reference_number generic_string not null unique,
    order_id integer not null references orders(id) on update restrict on delete restrict,
    origin_id integer not null references order_line_item_origins(id) on update restrict on delete restrict,
    origin_type character varying(255) not null,
    state character varying(255) not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    constraint valid_origin_type check (origin_type in ('skuItem', 'giftCardItem')),
    constraint valid_state check (state in ('cart', 'pending', 'preOrdered', 'backOrdered', 'canceled', 'shipped'))
);

create index order_line_items_order_and_origin_idx on order_line_items (order_id, origin_id);

create function set_oli_refnum() returns trigger as $$
begin
    if length(new.reference_number) = 0 then
        new.reference_number = uuid_in(md5(random()::text || now()::text)::cstring);
    end if;
    return new;
end;
$$ language plpgsql;

create trigger set_oli_refnum_trg
    before insert
    on order_line_items
    for each row
    execute procedure set_oli_refnum();