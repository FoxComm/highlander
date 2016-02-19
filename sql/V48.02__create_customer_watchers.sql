create table customer_watchers (
    id serial primary key,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    customer_id integer not null references customers(id) on update restrict on delete restrict,
    watcher_id integer not null references store_admins(id) on update restrict on delete restrict
)
