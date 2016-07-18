create table store_credit_manuals (
    id integer primary key,
    admin_id integer not null,
    reason_id integer not null,
    sub_reason_id integer null references reasons(id) on update restrict on delete restrict,
    created_at generic_timestamp,
    foreign key (id) references store_credit_origins(id) on update restrict on delete restrict,
    foreign key (admin_id) references store_admins(id) on update restrict on delete restrict,
    foreign key (reason_id) references reasons(id) on update restrict on delete restrict
);

create index store_credit_manuals_admin_idx on store_credit_manuals (admin_id);

create trigger set_store_credits_manuals_id
    before insert
    on store_credit_manuals
    for each row
    execute procedure set_store_credit_origin_id();

