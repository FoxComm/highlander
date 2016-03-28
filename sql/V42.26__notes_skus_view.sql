create materialized view notes_skus_view as
select
    n.id,
    -- SKU
    case when count(s) = 0
    then
        null
    else
        to_json((
            s.id,
            s.code,
            f.attributes,
            to_char(s.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
        )::export_skus_raw)
    end as sku
from notes as n
left join skus as s on (n.reference_id = s.id AND n.reference_type = 'sku')
left join object_forms as f on f.id = s.form_id
group by n.id, s.id, f.id;

create unique index notes_skus_view_idx on notes_skus_view (id);
