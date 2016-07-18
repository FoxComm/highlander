create materialized view notes_coupons_view as
select
    n.id,
    -- Coupon
    case when count(c) = 0
    then
        null
    else
        to_json((
            f.id,
            c.promotion_id,
            f.attributes,
            to_char(c.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
        )::export_coupons_raw)
    end as coupon
from notes as n
left join coupons as c on (c.form_id = n.reference_id and n.reference_type = 'coupon')
left join object_forms as f on (f.id = c.form_id and f.kind = 'coupon')
group by n.id, c.id, f.id
order by id;

create unique index notes_coupons_view_idx on notes_coupons_view (id);
