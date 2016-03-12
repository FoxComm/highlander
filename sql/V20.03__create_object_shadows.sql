create table object_shadows(
    id serial primary key,
    form_id integer not null references object_forms(id) on update restrict on delete restrict,
    attributes jsonb,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (form_id) references object_forms(id) on update restrict on delete restrict
);

create index object_shadows_object_form_idx on object_shadows (form_id);
