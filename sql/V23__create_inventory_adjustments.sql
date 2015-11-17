-- ledger for all changes inventory much like an accounting GL
create table inventory_adjustments (
    id bigserial primary key,
    warehouse_id integer not null,
    sku_id bigint not null,
    event_id bigint not null default 0,
    on_hand integer not null default 0,
    on_hold integer not null default 0,
    reserved integer not null default 0,
    non_sellable integer not null default 0,
    note_id integer, 
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (sku_id) references skus(id) on update restrict on delete restrict,
    foreign key (warehouse_id) references warehouses(id) on update restrict on delete restrict,
    foreign key (note_id) references notes(id) on update restrict on delete restrict
);

create trigger update_inventory_summaries 
    after insert
    on inventory_adjustments 
    for each row 
    execute procedure update_inventory_summaries();

