create table activity_dimensions(
    id serial primary key,
    name generic_string not null,
    description generic_string not null
);

create index activity_dimension_name_idx on activity_dimensions (name);
