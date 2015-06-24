-- ledger for all changes (increments/decrements) to inventory much like an accounting GL
create table inventory_adjustments (
    id bigserial primary key,
    sku_id bigint not null,
    inventory_event_id bigint not null,
    reserved_for_fulfillment integer not null default 0,
    fulfilled integer not null default 0,
    available_pre_order integer not null default 0,
    available_back_order integer not null default 0,
    outstanding_pre_orders integer not null default 0,
    outstanding_back_orders integer not null default 0,
    description character varying(255),
    source_notes text,
    foreign key (sku_id) references skus(id) on update restrict on delete restrict,
    foreign key (inventory_event_id) references inventory_events(id) on update restrict on delete restrict
);

create trigger update_inventory_summaries
    after insert
    on inventory_adjustments
    for each row
    execute procedure update_inventory_summaries();

