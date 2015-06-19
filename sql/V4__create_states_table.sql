create table states (
    id serial primary key,
    name character varying(255) not null,
    abbreviation character(2) not null
);

create index states_name_idx on states (name)

