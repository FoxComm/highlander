create table order_assignments (
    id serial primary key,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    order_id integer not null references orders(id) on update restrict on delete restrict,
    assignee_id integer not null references store_admins(id) on update restrict on delete restrict
)
