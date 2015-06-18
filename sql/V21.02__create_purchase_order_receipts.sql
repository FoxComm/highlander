--Subclass/Sub-table of inventory_adjustment
create purchase_order_receipts(
    inventory_adjustment_id int not null,
    purchase_order_id integer not null,
    receiver_name character varying(255), -- just for fun
    inventory_location_id int, -- just for fun
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null
);


alter table only purchase_orders_receipts
      add constraint purchase_order_receipts_pkey primary key (inventory_adjustment_id);

alter table only purchase_order_receipts
    add constraint purchase_order_receipts_inventory_adjustment_id_fk foreign key (inventory_adjustment_id) references inventory_adjustments (id) on update restrict on delete cascade;