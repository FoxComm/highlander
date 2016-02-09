create table shared_search_associations (
    id serial primary key,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    shared_search_id integer not null references shared_searches(id) on update restrict on delete restrict,
    store_admin_id integer not null references store_admins(id) on update restrict on delete restrict
);

create index shared_search_associations_search_id_idx on shared_search_associations (shared_search_id);
create index shared_search_associations_admin_id_idx on shared_search_associations (store_admin_id);