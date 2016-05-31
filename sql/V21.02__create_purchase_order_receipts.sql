--Subclass/Sub-table of inventory_adjustment
create table purchase_order_receipts (
    id bigint primary key,
    purchase_order_id integer not null,
    receiver_name generic_string, -- just for fun
    inventory_location_id int, -- just for fun
    created_at generic_timestamp,
    updated_at generic_timestamp,
    deleted_at timestamp without time zone null,
    foreign key (id) references inventory_events(id) on update restrict on delete restrict,
    foreign key (purchase_order_id) references purchase_orders(id) on update restrict on delete restrict
);

create trigger set_inventory_id_trigger
    before insert
    on purchase_order_receipts
    for each row
    execute procedure set_inventory_event_id();

