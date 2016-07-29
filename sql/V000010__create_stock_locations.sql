create table stock_locations (
    id serial primary key,
    type stock_location_type,
    name generic_string not null,
    address jsonb,

    created_at generic_timestamp_now,
    updated_at generic_timestamp_now,
    deleted_at generic_timestamp_null
);
