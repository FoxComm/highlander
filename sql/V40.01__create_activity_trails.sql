create table activity_trails(
    id serial primary key,
    dimension_id integer,
    tail_connection_id integer null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (dimension_id) references activity_dimensions(id) on update restrict on delete restrict
);

create index activity_dimension_idx on activity_trails(dimension_id);
