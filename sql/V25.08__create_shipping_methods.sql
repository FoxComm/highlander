create table shipping_methods(
    id serial primary key,
    -- parent_id: editing a shipping method creates a new version which points to the original parent.
    -- This allows us to reference a given shipping method for an Order while preserving shipping information
    -- as an immutable fact.
    parent_id integer null references shipping_methods(id) on update restrict on delete restrict,
    admin_display_name generic_string not null,
    storefront_display_name generic_string not null,
    shipping_carrier_id integer, --Nullable because we may ship ourselves?
    price integer not null,
    is_active boolean not null default false,
    conditions jsonb null,
    restrictions jsonb null
);

create index shipping_methods_active_idx on shipping_methods (is_active) where is_active = true;