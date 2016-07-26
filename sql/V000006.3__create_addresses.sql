create table addresses (
  id serial primary key,
  region_id integer not null references regions(id) on update restrict on delete restrict,
  name generic_string not null,
  address1 generic_string not null,
  address2 generic_string null,
  city generic_string not null,
  zip zip_code not null,
  phone_number phone_number null,

  created_at generic_timestamp_now,
  updated_at generic_timestamp_now,
  deleted_at generic_timestamp_null,

  foreign key (region_id) references regions(id) on update restrict on delete restrict
);
