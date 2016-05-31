create table activities(
    id serial primary key,
    activity_type generic_string,
    data jsonb,
    context jsonb,
    created_at generic_timestamp
);

create index activity_type_idx on activities (activity_type);
