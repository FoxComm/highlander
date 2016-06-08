create materialized view activity_connections_view as
select
    a.id,
    a.dimension_id,
    a.trail_id,
    a.activity_id,
    a.data,
    a.connected_by,
    to_char(a.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at
from 
    activity_connections a;

create unique index activity_connections_view_idx on activity_connections_view (id);
