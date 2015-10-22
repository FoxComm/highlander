create table rma_reasons (
    id serial primary key,
    name generic_string not null,
    type rma_reason_type not null,
    default_type rma_type not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone
);