create table order_weight_criteria(
      id integer primary key,
      greater_than integer,
      less_than integer,
      exact_match integer,
      unit_of_measure generic_string not null,
      exclude boolean,
      foreign key (id) references order_criteria(id) on update restrict on delete restrict
  );