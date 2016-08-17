create table captures
(
    id bigint not null unique,
    ref reference_number not null unique,
    order_ref reference_number not null references orders(reference_number) on update restrict on delete restrict
    customer_id bigint not null references customers(id) on update restrict on delete
    event varying(128) not null,
    amount integer not null default 0,
    currency currency not null,
    error generic_string,
    created_at generic_timestamp
);

create index captures_ref_idx on captures(ref, order_ref, customer_id);
