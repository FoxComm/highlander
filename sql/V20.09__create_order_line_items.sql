create table order_line_items (
    id serial primary key,
    reference_number generic_string not null unique,
    order_ref text not null references orders(reference_number) on update restrict on delete restrict,
    origin_id integer not null references order_line_item_origins(id) on update restrict on delete restrict,
    origin_type character varying(255) not null,
    state character varying(255) not null,
    created_at generic_timestamp,
    constraint valid_origin_type check (origin_type in ('skuItem', 'giftCardItem')),
    constraint valid_state check (state in ('cart', 'pending', 'preOrdered', 'backOrdered', 'canceled', 'shipped'))
);

create index order_line_items_order_and_origin_idx on order_line_items (order_ref, origin_id);

create function set_oli_refnum() returns trigger as $$
begin
    if length(new.reference_number) = 0 then
        new.reference_number = md5(random()::text || clock_timestamp()::text)::uuid::text;
    end if;
    return new;
end;
$$ language plpgsql;

create trigger set_oli_refnum_trg
    before insert
    on order_line_items
    for each row
    execute procedure set_oli_refnum();