create table search_indexes(
    id serial primary key,
    name generic_string not null unique,
    scope exts.ltree not null,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    deleted_at timestamp without time zone null
);


create table search_fields(
    id serial primary key,
    name generic_string not null,
    analyzer generic_string not null,
    index_id integer not null references search_indexes(id) on update restrict on delete restrict,
    created_at generic_timestamp,
    updated_at generic_timestamp,
    deleted_at timestamp without time zone null
);

create index search_fields_name_idx on search_fields (name);
create index search_fields_index_idx on search_fields (index_id);
