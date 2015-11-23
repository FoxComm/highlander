create table rma_line_items (
    id serial primary key,
    rma_id integer not null references rmas(id) on update restrict on delete restrict,
    reason_id integer not null references rma_reasons(id) on update restrict on delete restrict,
    quantity integer not null,
    origin_id integer not null references rma_line_item_origins(id) on update restrict on delete restrict,
    origin_type rma_line_item_origin_type not null,
    is_return_item boolean not null default false,
    inventory_disposition rma_inventory_disposition not null,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create index rma_line_items_rma_id_and_origin_idx on rma_line_items (rma_id, origin_id)