create table object_contexts(
    id serial primary key,
    name generic_string,
    attributes jsonb,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create unique index object_contexts_idx on object_contexts (id);
create unique index object_contexts_namex on object_contexts (name);

