create table vendors (
  id serial primary key,
  name generic_string,
  description note_body,
  tax_id generic_string,
  is_disabled boolean no null default false,
  created_at generic_timestamp,
  updated_at generic_timestamp,
  deleted_at timestamp without time zone null
)

