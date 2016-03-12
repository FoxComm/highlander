create table form_commits(
    id serial primary key,
    shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
    form_id integer not null references object_forms(id) on update restrict on delete restrict,
    reason_id integer references reasons(id) on update restrict on delete restrict,
    previous_id integer references form_commits(id) on update restrict on delete restrict,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create index form_commits_form_idx on form_commits (form_id);

