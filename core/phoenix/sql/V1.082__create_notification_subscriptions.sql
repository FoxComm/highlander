create table notification_subscriptions (
  id serial primary key,
  admin_id int references store_admins(id) on update restrict on delete restrict,
  dimension_id int references activity_dimensions(id) on update restrict on delete restrict,
  object_id generic_string not null,
  created_at generic_timestamp,
  reason text
);

create unique index notification_sub_idx on notification_subscriptions (admin_id, dimension_id, object_id, reason);
