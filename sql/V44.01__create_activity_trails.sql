create table activity_trails(
    id serial primary key,
    dimension_id integer,
    object_id generic_string, 
    tail_connection_id integer null,
    data jsonb null,
    created_at generic_timestamp,
    foreign key (dimension_id) references activity_dimensions(id) on update restrict on delete restrict
);

create index activity_dimension_idx on activity_trails(dimension_id);
create index activity_object_id_idx on activity_trails(object_id);
