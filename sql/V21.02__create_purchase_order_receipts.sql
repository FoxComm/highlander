--Subclass/Sub-table of inventory_adjustment
create table purchase_order_receipts (
    id bigint primary key,
    purchase_order_id integer not null,
    receiver_name character varying(255), -- just for fun
    inventory_location_id int, -- just for fun
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    foreign key (id) references inventory_events(id) on update restrict on delete restrict
    foreign key (purchase_order_id) references purchase_orders(id) on update restrict on delete restrict
);