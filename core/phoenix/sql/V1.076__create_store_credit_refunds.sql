create table store_credit_refunds (
    id integer primary key,
    return_id integer not null,
    created_at generic_timestamp,
    foreign key (id) references store_credit_origins(id) on update restrict on delete restrict,
    foreign key (return_id) references returns(id) on update restrict on delete restrict
);

create index store_credit_refunds_return_idx on store_credit_refunds (return_id);

create trigger set_store_credits_refunds_id
    before insert
    on store_credit_refunds
    for each row
    execute procedure set_store_credit_origin_id();

