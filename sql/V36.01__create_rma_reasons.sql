create table rma_reasons (
    id serial primary key,
    name generic_string not null,
    reason_type rma_reason_type not null,
    rma_type rma_type not null,
    created_at generic_timestamp,
    deleted_at timestamp without time zone
);