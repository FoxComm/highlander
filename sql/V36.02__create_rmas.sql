create table rmas (
    id serial primary key,
    reference_number reference_number not null unique,
    order_id integer not null references orders(id) on update restrict on delete restrict,
    order_refnum reference_number not null,
    rma_type rma_type not null,
    state rma_state not null,
    is_locked boolean default false,
    message_to_customer text null,
    customer_id integer not null references customers(id) on update restrict on delete restrict,
    store_admin_id integer null references store_admins(id) on update restrict on delete restrict,
    canceled_reason integer null references reasons(id) on update restrict on delete restrict,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    deleted_at timestamp without time zone null
);

create index rmas_order_id_and_state on rmas (order_id, state);

create function set_rmas_reference_number() returns trigger as $$
begin
    if length(new.reference_number) = 0 then
        new.reference_number = concat(new.order_refnum, '.', next_rma_id(new.order_id));
    end if;
    return new;
end;
$$ language plpgsql;

create trigger set_rmas_reference_number_trg
    before insert
    on rmas
    for each row
    execute procedure set_rmas_reference_number();