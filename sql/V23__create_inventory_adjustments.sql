create table inventory_adjustments (
    id serial primary key,
    sku_id bigint not null,
    inventory_event_id bigint not null,
    reserved_for_fulfillment integer not null,
    fulfilled integer not null,
    available_pre_order integer not null,
    available_back_order integer not null,
    outstanding_pre_orders integer not null,
    outstanding_back_orders integer not null,
    description character varying(255),
    source_notes text,
    foreign key (sku_id) references skus(id) on update restrict on delete restrict,
    foreign key (inventory_event_id) references inventory_events(id) on update restrict on delete restrict
 );
