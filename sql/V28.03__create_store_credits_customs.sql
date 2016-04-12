create table store_credit_customs (
    id integer primary key,
    admin_id integer not null,
    metadata jsonb null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (id) references store_credit_origins(id) on update restrict on delete restrict,
    foreign key (admin_id) references store_admins(id) on update restrict on delete restrict
);

create index store_credit_customs_admin_idx on store_credit_customs (admin_id);

create trigger set_store_credits_customs_id
    before insert
    on store_credit_customs
    for each row
    execute procedure set_store_credit_origin_id();

