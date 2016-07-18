create table customer_dynamic_groups (
    id serial primary key,
    created_by integer not null references store_admins(id) on update restrict on delete restrict,
    name generic_string not null unique,
    customers_count integer null,
    client_state jsonb not null,
    elastic_request jsonb not null,
    updated_at generic_timestamp,
    created_at generic_timestamp
);
