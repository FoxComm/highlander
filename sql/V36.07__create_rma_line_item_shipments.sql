create table rma_line_item_shipments (
    id integer primary key,
    rma_id integer not null references rmas(id) on update restrict on delete restrict,
    shipment_id integer not null references shipments(id) on update restrict on delete restrict,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (id) references rma_line_item_origins(id) on update restrict on delete restrict
);

create index rma_line_item_shipments_shipment_idx on rma_line_item_shipments (shipment_id);
create index rma_line_item_shipments_rma_idx on rma_line_item_shipments (rma_id);

create trigger set_rma_line_item_shipment_id
    before insert
    on rma_line_item_shipments
    for each row
    execute procedure set_rma_line_item_origin_id();

