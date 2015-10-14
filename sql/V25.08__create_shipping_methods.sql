create table shipping_methods(
    id serial primary key,
    admin_display_name generic_string not null,
    storefront_display_name generic_string not null,
    shipping_carrier_id integer, --Nullable because we may ship ourselves?
    price integer not null,
    is_active boolean not null default false,
    conditions jsonb null,
    restrictions jsonb null
);