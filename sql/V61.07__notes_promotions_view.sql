create materialized view notes_promotions_view as
select
    n.id,
    -- Promotion
    case when count(p) = 0
    then
        null
    else
        to_json((
            f.id,
            p.apply_type,
            f.attributes,
            to_char(p.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
        )::export_promotions_raw)
    end as promotion
from notes as n
left join promotions as p on (n.reference_id = p.id AND n.reference_type = 'promotion')
left join object_forms as f on f.id = p.form_id
group by n.id, p.id, f.id;

create unique index notes_promotions_view_idx on notes_promotions_view (id);
