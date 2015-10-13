create table order_dimension_criteria(
    id integer primary key,
    dimension_type generic_string not null,
    greater_than integer,
    less_than integer,
    exact_match integer,
    unit_of_measure generic_string not null,
    exclude boolean,
    foreign key (id) references order_criteria(id) on update restrict on delete restrict
);