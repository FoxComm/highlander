create table order_destination_criteria(
    id integer primary key,
    destination_type character varying(255) not null,
    destination json,
    exclude boolean,
    foreign key (id) references order_criteria(id) on update restrict on delete restrict
);