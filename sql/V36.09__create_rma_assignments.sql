create table rma_assignments (
    id serial primary key,
    assigned_at timestamp without time zone default (now() at time zone 'utc'),
    rma_id integer not null references rmas(id) on update restrict on delete restrict,
    assignee_id integer references store_admins(id) on update restrict on delete restrict
);

create index rma_assignments_rma_id_idx on rma_assignments (rma_id);