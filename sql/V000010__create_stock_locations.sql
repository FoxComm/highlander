create table stock_locations (
    id serial primary key,
    type stock_location_type,
    name generic_string not null,
    address jsonb not null
);
