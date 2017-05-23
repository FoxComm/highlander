create table channels (
    id serial primary key,
    organization_id integer
);

create table host_map (
    id serial primary key,
    host varchar(255) unique,
    channel_id integer not null references channels(id),
    scope ltree not null
);
