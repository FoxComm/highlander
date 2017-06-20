create table channels_search_view (
  id bigint primary key,
  scope exts.ltree not null,
  name generic_string not null,
  hosts jsonb default '[]',
  organization_name generic_string not null,
  purchase_location generic_string not null,
  created_at json_timestamp,
  updated_at json_timestamp
);
