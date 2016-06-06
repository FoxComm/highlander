create table return_lock_events (
    id serial primary key,
    return_id integer references returns(id) on update restrict on delete restrict,
    locked_at generic_timestamp,
    locked_by int references store_admins(id) on update restrict on delete restrict
);

create index return_lock_events_return_id_idx on return_lock_events (return_id);