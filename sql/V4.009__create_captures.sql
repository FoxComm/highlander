create table captures
(
    id bigint not null,
    order_ref reference_number not null references orders(reference_number) on update restrict on delete restrict,
    customer_id bigint not null references customers(id) on update restrict on delete restrict,
    event character varying(128) not null,
    amount integer not null default 0,
    currency currency not null,
    error generic_string,
    created_at generic_timestamp
);

create index captures_ref_idx on captures(customer_id, order_ref);
