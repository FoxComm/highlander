create table activities(
    id serial primary key,
    activity_type generic_string,
    data jsonb,
    triggered_by jsonb,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create index activity_type_idx on activities (activity_type);
