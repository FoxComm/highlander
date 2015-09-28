create table order_assignments (
    id serial primary key,
    assigned_at timestamp without time zone default (now() at time zone 'utc'),
    order_id integer references orders(id) on update restrict on delete restrict,
    assignee_id integer references store_admins(id) on update restrict on delete restrict
)
