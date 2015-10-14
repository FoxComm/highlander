create table order_price_criteria(
    id integer primary key,
    price_type generic_string not null,
    greater_than integer,
    less_than integer,
    exact_match integer,
    currency currency,
    exclude boolean,
    foreign key (id) references order_criteria(id) on update restrict on delete restrict
);