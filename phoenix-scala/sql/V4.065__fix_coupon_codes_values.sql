update coupons_search_view set
  codes = q.codes from (select
    cp.form_id,
    jsonb_agg(c.code) as codes
  from coupons as cp
  left join coupon_codes as c on (cp.form_id = c.coupon_form_id)
  group by cp.form_id) as q
  where coupons_search_view.id = q.form_id;

