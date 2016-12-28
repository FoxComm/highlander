create table external_freights (
  id serial primary key,
  carrier_id integer not null references carriers on update restrict on delete restrict,
  method_name generic_string not null,
  service_code generic_string not null,

  created_at generic_timestamp_now,
  updated_at generic_timestamp_now,
  deleted_at generic_timestamp_null,

  foreign key (carrier_id) references carriers(id) on update restrict on delete restrict
);
