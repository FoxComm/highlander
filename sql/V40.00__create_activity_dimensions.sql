create table activity_dimensions(
    id serial primary key,
    name generic_string,
    description generic_string
);

create index activity_dimension_name_idx on activity_dimensions (name);
