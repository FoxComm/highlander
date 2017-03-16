
create table notifications(
    id serial primary key,
    scope exts.ltree not null,
    account_id integer not null references accounts(id) on update restrict on delete restrict,
    dimension_id integer not null references activity_dimensions(id) on update restrict on delete restrict,
    object_id generic_string not null,
    activity jsonb not null,
    created_at generic_timestamp not null
);

create index notifications_idx on notifications(scope, account_id);

create table last_seen_notifications(
    id serial primary key,
    scope exts.ltree not null,
    account_id integer not null references accounts(id) on update restrict on delete restrict,
    notification_id integer not null references notifications(id) on update restrict on delete restrict
);

create index last_seen_notifications_idx on last_seen_notifications(scope, account_id);

