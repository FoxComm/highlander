create table default_shipping_methods (
  id serial primary key,
  scope ltree not null unique,
  shipping_method_id integer not null references shipping_methods(id) on update cascade on delete cascade
);
