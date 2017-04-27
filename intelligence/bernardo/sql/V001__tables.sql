create extension ltree;

create type distance_function as enum('euclidean', 'hamming');

create table cluster_definitions(
  id serial primary key,
  distance_func distance_function
);

create type trait_kind as enum('number', 'enumeration');

create table trait_definitions(
  id serial primary key,
  cluster_definition_id integer references cluster_definitions(id),
  name text,
  kind trait_kind,
  enum_values text[]
);

create table groups(
  id serial primary key,
  scope ltree,
  name text,
  cluster_definition_id integer references cluster_definitions(id)
);

create table clusters(
  id serial primary key,
  group_id integer references groups(id),
  ref text,
  traits jsonb
);

