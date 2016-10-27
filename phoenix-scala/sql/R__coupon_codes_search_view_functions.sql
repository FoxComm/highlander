create or replace function update_coupon_codes_view_insert_fn() returns trigger as $$
  begin
    insert into coupon_codes_search_view select distinct on (new.id)
      c.id,
      c.code,
      cp.form_id as coupon_id,
      cp.promotion_id,
      0 as total_used,
      to_char(c.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
      cp.scope as scope
    from coupon_codes as c
    inner join coupons as cp on (cp.form_id = c.coupon_form_id)
    where c.id = new.id;

    return null;
  end;
$$ language plpgsql;


create or replace function update_coupon_codes_view_from_coupons_fn() returns trigger as $$
  begin
    update coupon_codes_search_view set
      coupon_id = q.form_id,
      promotion_id = q.promotion_id,
      scope = q.scope
      from (select
        cp.form_id,
        cp.promotion_id,
        cp.scope
        from coupons as cp
        where cp.id = new.id) as q
      where coupon_codes_search_view.coupon_id = q.form_id;
return null;
end;
$$ language plpgsql;

