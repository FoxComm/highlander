create table cycle_counts (
    id bigint primary key,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    deleted_at timestamp without time zone null,
    foreign key (id) references inventory_events(id) on update restrict on delete restrict
);

create trigger set_inventory_id_trigger
    before insert
    on cycle_counts
    for each row
    execute procedure set_inventory_event_id();

