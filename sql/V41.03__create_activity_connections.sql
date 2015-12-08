create table activity_connections(
    id serial primary key,
    dimension_id integer,
    trail_id integer,
    activity_id integer,
    previous_id integer null,
    next_id integer null,
    data jsonb null,
    connected_by jsonb,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (dimension_id) references activity_dimensions(id) on update restrict on delete restrict,
    foreign key (activity_id) references activities(id) on update restrict on delete restrict,
    foreign key (next_id) references activity_connections(id) on update restrict on delete restrict,
    foreign key (previous_id) references activity_connections(id) on update restrict on delete restrict
);

create index activity_connection_dimension_idx on activity_connections (dimension_id);
create index activity_connection_trail_id_idx on activity_connections (trail_id);

alter table activity_trails add foreign key(tail_connection_id) references activity_connections(id);
