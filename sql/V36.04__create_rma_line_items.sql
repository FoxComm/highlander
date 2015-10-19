create table rma_line_items (
    id serial primary key,
    rma_id integer not null references rmas(id) on update restrict on delete restrict,
    reason_id integer not null references rma_reasons(id) on update restrict on delete restrict,
    origin_id integer not null references rma_line_item_origins(id) on update restrict on delete restrict,
    origin_type generic_string not null,
    type rma_type not null,
    inventory_disposition rma_inventory_disposition not null,
    status rma_status not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    constraint valid_origin_type check (origin_type in ('skuItem', 'shipment'))
);

create index rma_line_items_rma_id_and_origin_idx on rma_line_items (rma_id, origin_id)