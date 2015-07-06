create table store_credit_csrs (
    id integer primary key,
    admin_id integer not null,
    reason character varying(255) not null,
    sub_reason character varying(255) null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (id) references store_credit_origins(id) on update restrict on delete restrict,
    foreign key (admin_id) references store_admins(id) on update restrict on delete restrict
);

create index store_credit_csrs_idx on store_credit_csrs (admin_id);

create trigger set_store_credits_csrs_id
    before insert
    on store_credit_csrs
    for each row
    execute procedure set_store_credit_origin_id();

