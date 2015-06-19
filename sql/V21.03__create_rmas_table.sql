create table rmas (
    id serial primary key,
    order_id int not null,
    reason character varying(255), -- Placeholder
    source character varying(255), -- Placeholder
    foreign key (order_id) references orders(id) on update restrict on delete restrict
);