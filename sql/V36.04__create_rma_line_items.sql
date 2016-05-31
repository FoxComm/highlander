create table rma_line_items (
    id serial primary key,
    reference_number generic_string not null unique,
    rma_id integer not null references rmas(id) on update restrict on delete restrict,
    reason_id integer not null references rma_reasons(id) on update restrict on delete restrict,
    quantity integer not null,
    origin_id integer not null references rma_line_item_origins(id) on update restrict on delete restrict,
    origin_type rma_line_item_origin_type not null,
    is_return_item boolean not null default false,
    inventory_disposition rma_inventory_disposition not null,
    created_at generic_timestamp
);

create index rma_line_items_rma_id_and_origin_idx on rma_line_items (rma_id, origin_id);

create function set_rli_refnum() returns trigger as $$
begin
    if length(new.reference_number) = 0 then
        new.reference_number = md5(random()::text || clock_timestamp()::text)::uuid::text;
    end if;
    return new;
end;
$$ language plpgsql;

create trigger set_rli_refnum_trg
    before insert
    on rma_line_items
    for each row
    execute procedure set_rli_refnum();	