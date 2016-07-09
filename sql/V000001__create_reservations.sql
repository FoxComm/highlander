create table reservations (
  id serial primary key,
  ref_num generic_string not null,

  created_at generic_timestamp,
  updated_at generic_timestamp,
  deleted_at timestamp without time zone null
);

create unique index reservations_ref_num_idx on reservations (lower(ref_num));
