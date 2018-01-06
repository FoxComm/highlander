create table channels (
  id serial primary key,
  intelligence_channel_id integer not null,
  scope exts.ltree not null,
  name generic_string not null,
  purchase_location integer not null,
  created_at generic_timestamp not null,
  updated_at generic_timestamp not null
);
