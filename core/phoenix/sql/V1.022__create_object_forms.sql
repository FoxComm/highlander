
create table object_forms (
    id serial primary key,
    kind generic_string,
    attributes jsonb,
    updated_at generic_timestamp,
    created_at generic_timestamp
);

create index object_shadows_object_kndx on object_forms (kind);
