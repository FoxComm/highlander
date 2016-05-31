create table object_commits(
    id serial primary key,
    form_id integer not null references object_forms(id) on update restrict on delete restrict,
    shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
    reason_id integer references reasons(id) on update restrict on delete restrict,
    previous_id integer references object_commits(id) on update restrict on delete restrict,
    created_at generic_timestamp
);

create index object_commits_form_idx on object_commits (form_id);

