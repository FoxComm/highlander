create table order_dimension_criteria(
    id integer primary key,
    dimension_type character varying(255) not null,
    greater_than integer,
    less_than integer,
    exact_match integer,
    unit_of_measure character varying(255) not null,
    exclude boolean,
    foreign key (id) references order_criteria(id) on update restrict on delete restrict
);