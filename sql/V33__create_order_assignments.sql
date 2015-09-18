create table order_assignments (
    id serial primary key,
    order_id integer references orders(id) on update restrict on delete restrict,
    assignee_id integer references store_admins(id) on update restrict on delete restrict
)
