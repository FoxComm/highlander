create table regions (
    id serial primary key,
    country_id integer not null references countries(id) on update restrict on delete restrict,
    name generic_string not null,
    abbreviation region_abbr null
);

create index regions_name_idx on regions (name);

