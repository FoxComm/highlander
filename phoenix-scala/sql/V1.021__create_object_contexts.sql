create table object_contexts(
    id serial primary key,
    name generic_string,
    attributes jsonb,
    created_at generic_timestamp
);

create unique index object_contexts_idx on object_contexts (id);
create unique index object_contexts_namex on object_contexts (name);

