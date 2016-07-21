create table activity_connections_view
(
    id bigint not null unique,
    dimension_id integer not null,
    trail_id integer,
    activity_id integer,
    data jsonb null,
    connected_by jsonb not null,
    created_at text
);

create or replace function update_activity_connections_view_insert_fn() returns trigger as $$
    begin
        insert into activity_connections_view select distinct on (new.id)
            -- customer
            new.id as id,
            new.dimension_id as dimension_id,
            new.trail_id as trail_id,
            new.activity_id as activity_id,
            new.data as data,
            new.connected_by as connected_by,
            to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at
            from customers as c;
      return null;
  end;
$$ language plpgsql;

create or replace function update_activity_connections_view_update_fn() returns trigger as $$
begin
    update activity_connections_view set
        dimension_id = new.dimension_id,
        trail_id = new.trail_id,
        activity_id = new.activity_id,
        data = new.data,
        connected_by = new.connected_by,      
        created_at = to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
    where id = new.id;
    return null;
    end;
$$ language plpgsql;

create trigger update_activity_connections_view_insert
    after insert on activity_connections
    for each row
    execute procedure update_activity_connections_view_insert_fn();

create trigger update_activity_connections_view_update
    after update on activity_connections
    for each row
    execute procedure update_activity_connections_view_update_fn();
