create materialized view notes_customers_view as
select
    n.id,
    -- Customer
    case when count(c) = 0
    then
        null
    else
        to_json((
            c.id,
            c.name,
            c.email,
            c.is_blacklisted,
            to_char(c.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
        )::export_customers)
    end as customer
from notes as n
left join customers as c on (n.reference_id = c.id AND n.reference_type = 'customer')
group by n.id, c.id;

create unique index notes_customers_view_idx on notes_customers_view (id);
