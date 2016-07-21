-- table not used yet
create table reservations (
  id serial primary key,
  ref_num generic_string not null,

  created_at generic_timestamp_now,
  updated_at generic_timestamp_now,
  deleted_at generic_timestamp_null
);

create unique index reservations_ref_num_idx on reservations (lower(ref_num));
