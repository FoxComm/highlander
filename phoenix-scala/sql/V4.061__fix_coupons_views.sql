alter table coupon_codes_search_view add column scope exts.ltree;

update coupon_codes_search_view
  set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);

alter table coupon_codes_search_view alter column scope set not null;

alter table coupons_search_view add column codes jsonb;

update coupons_search_view set
  codes = q.codes from (select
    cp.form_id,
    jsonb_agg(c.code) filter (where c.code is not null) over (partition by cp.form_id) as codes
  from coupons as cp
  left join coupon_codes as c on (cp.form_id = c.coupon_form_id)) as q
  where coupons_search_view.id = q.form_id;


create or replace function update_coupons_view_update_codes_fn() returns trigger as $$
begin
    update coupons_search_view set
      codes = q.codes
      from (select
              cp.form_id,
              jsonb_agg(c.code) filter (where c.code is not null) over (partition by cp.form_id)  as codes
            from coupons as cp
            left join coupon_codes as c on (cp.form_id = c.coupon_form_id)
            where c.id = new.id) as q
    where coupons_search_view.id = q.form_id;

    return null;
    end;
$$ language plpgsql;

create trigger update_coupon_view_on_codes
    after insert on coupon_codes
    for each row
    execute procedure update_coupons_view_update_codes_fn();

