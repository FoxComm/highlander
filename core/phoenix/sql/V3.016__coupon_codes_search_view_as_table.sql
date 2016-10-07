drop materialized view coupon_codes_search_view;

create table coupon_codes_search_view
(
    id bigint not null unique,
    code generic_string,
    coupon_id integer,
    promotion_id integer,
    total_used integer,
    created_at json_timestamp
);

create or replace function update_coupon_codes_view_insert_fn() returns trigger as $$
  begin
    insert into coupon_codes_search_view select distinct on (new.id)
      c.id,
      c.code,
      cp.form_id as coupon_id,
      cp.promotion_id,
      0 as total_used,
      to_char(c.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at
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
      promotion_id = q.promotion_id
      from (select
        cp.form_id,
        cp.promotion_id
        from coupons as cp
        where cp.id = new.id) as q
      where coupon_codes_search_view.coupon_id = q.form_id;
return null;
end;
$$ language plpgsql;


create or replace function update_coupon_codes_view_totals_fn() returns trigger as $$
begin
  update coupon_codes_search_view set
    total_used = new.count
    where coupon_codes_search_view.id = new.coupon_code_id;
  return null;
  end;
$$ language plpgsql;

create trigger update_coupon_codes_view_insert
    after insert on coupon_codes
    for each row
    execute procedure update_coupon_codes_view_insert_fn();

create trigger update_coupon_codes_view_from_coupons
  after update on coupons
  for each row
  execute procedure update_coupon_codes_view_from_coupons_fn();

create trigger update_coupon_codes_view_totals
  after update on coupon_code_usages
  for each row
  execute procedure update_coupon_codes_view_totals_fn();
