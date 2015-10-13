create table rmas (
    id serial primary key,
    order_id int not null,
    reason generic_string, -- Placeholder
    source generic_string, -- Placeholder
    foreign key (order_id) references orders(id) on update restrict on delete restrict
);

