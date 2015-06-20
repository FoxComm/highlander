create table order_price_criteria(
    id primary key,
    price_type character varying(255) not null,
    greater_than integer,
    less_than integer,
    exact_match integer,
    currency character varying(255) not null,
    exclude boolean,
    foreign key (id) references order_criteria(id) on update restrict on delete restrict
);