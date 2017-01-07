create table resource_map(
  id serial primary key,
  cluster_id integer references clusters(id),
  res text,
  refs jsonb
);
