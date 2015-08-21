create table regions (
    id serial primary key,
    country_id integer not null references countries(id) on update restrict on delete restrict,
    name character varying(255) not null,
    abbreviation character varying(10) not null
);

create index regions_name_idx on regions (name);

