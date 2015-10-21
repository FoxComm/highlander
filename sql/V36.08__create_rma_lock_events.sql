create table rma_lock_events (
    id serial primary key,
    rma_id integer references rmas(id) on update restrict on delete restrict,
    locked_at timestamp without time zone default (now() at time zone 'utc'),
    locked_by int references store_admins(id) on update restrict on delete restrict
);

create index rma_lock_events_rma_id_idx on rma_lock_events (rma_id);