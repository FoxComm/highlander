create table rmas (
    id serial primary key,
    reference_number reference_number not null,
    order_id integer not null references orders(id) on update restrict on delete restrict,
    order_refnum reference_number not null,
    rma_type rma_type not null,
    status rma_status not null,
    locked boolean default false,
    customer_id integer null references customers(id) on update restrict on delete restrict,
    store_admin_id integer null references store_admins(id) on update restrict on delete restrict,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null
);

create index rmas_order_id_and_status on rmas (order_id, status);

create function set_rmas_reference_number() returns trigger as $$
declare
    order_rma_count integer default 0;
begin
    if length(new.reference_number) = 0 then
        select count(*) into order_rma_count from rmas where order_refnum = new.order_refnum;
        new.reference_number = concat(new.order_refnum, '.', order_rma_count + 1);
    end if;
    return new;
end;
$$ language plpgsql;

create trigger set_rmas_reference_number_trg
    before insert
    on rmas
    for each row
    execute procedure set_rmas_reference_number();