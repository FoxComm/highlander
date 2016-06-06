create table return_reasons (
    id serial primary key,
    name generic_string not null,
    reason_type return_reason_type not null,
    return_type return_type not null,
    created_at generic_timestamp,
    deleted_at timestamp without time zone
);