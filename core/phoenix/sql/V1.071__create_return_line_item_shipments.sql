create table return_line_item_shipments (
    id integer primary key,
    return_id integer not null references returns(id) on update restrict on delete restrict,
    shipment_id integer not null references shipments(id) on update restrict on delete restrict,
    created_at generic_timestamp,
    foreign key (id) references return_line_item_origins(id) on update restrict on delete restrict
);

create index return_line_item_shipments_shipment_idx on return_line_item_shipments (shipment_id);
create index return_line_item_shipments_return_idx on return_line_item_shipments (return_id);

create trigger set_return_line_item_shipment_id
    before insert
    on return_line_item_shipments
    for each row
    execute procedure set_return_line_item_origin_id();

