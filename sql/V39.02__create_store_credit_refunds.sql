create table store_credit_refunds (
    id integer primary key,
    rma_id integer not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (id) references store_credit_origins(id) on update restrict on delete restrict,
    foreign key (rma_id) references rmas(id) on update restrict on delete restrict
);

create index store_credit_refunds_rma_idx on store_credit_refunds (rma_id);

create trigger set_store_credits_refunds_id
    before insert
    on store_credit_refunds
    for each row
    execute procedure set_store_credit_origin_id();

