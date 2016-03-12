
create table object_forms (
    id serial primary key,
    kind generic_string,
    attributes jsonb,
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create index object_shadows_object_kndx on object_forms (object_kind);
