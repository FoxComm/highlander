create table inventory_adjustments (
    id integer not null,
    sku_id integer not null,
    order_id integer,
    purchase_order_receipt_id integer,
    rma_receipt_id integer,
    cycle_count_id integer,
    physical_inventory_event_id integer,
    on_hand_adjustment_id integer,
    available_pre_order integer not null,
    available_back_order integer not null,
    outstanding_pre_orders integer not null,
    outstanding_back_orders integer not null,
    description character varying(255),
    source_notes text 
);

create sequence inventory_adjustments_id_seq
     start with 1
     increment by 1
     no minvalue
     no maxvalue
     cache 1;


alter table only inventory_adjustments
    add constraint inventory_adjustments_pkey primary key (id);

alter table only inventory_adjustments
    alter column id set default nextval('inventory_adjustments_id_seq'::regclass);

alter table only inventory_adjustments
    add constraint inventory_adjustments_sku_id_fk foreign key (sku_id) references skus(id) on update restrict on delete restrict;

alter table only inventory_adjustments
    add constraint inventory_adjustments_order_id_fk foreign key (order_id) references orders(id) on update restrict on delete restrict;


--TODO: Create foreign key associations with other tables as they become created
