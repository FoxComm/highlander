create or replace function update_coupons_view_insert_fn() returns trigger as $$
  begin
    insert into coupons_search_view select distinct on (new.id)
      f.id,
      cp.promotion_id,
      context.name as context,
      illuminate_text(f, s, 'name') as name,
      illuminate_text(f, s, 'storefrontName') as storefront_name,
      illuminate_text(f, s, 'description') as description,
      illuminate_text(f, s, 'activeFrom') as active_from,
      illuminate_text(f, s, 'activeTo') as active_to,
      0 as total_used,
      to_char(f.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
      to_char(cp.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
      cp.scope as scope,
      jsonb_agg(c.code) filter (where c.code is not null) over (partition by f.id)  as codes,
      (illuminate_obj(f, s, 'usageRules')->>'usesPerCode') :: integer as max_uses_per_code,
      (illuminate_obj(f, s, 'usageRules')->>'usesPerCustomer') :: integer as max_uses_per_customer
      from coupons as cp
      left join coupon_codes as c on (cp.form_id = c.coupon_form_id)
      inner join object_contexts as context on (cp.context_id = context.id)
      inner join object_forms as f on (f.id = cp.form_id)
      inner join object_shadows as s on (s.id = cp.shadow_id)
      where cp.id = new.id;

      return null;
  end;
$$ language plpgsql;

create or replace function update_coupons_view_update_fn() returns trigger as $$
begin
    update coupons_search_view set
      id = q.id,
      promotion_id = q.promotion_id,
      context = q.context,
      name = q.name,
      storefront_name = q.storefront_name,
      description = q.description,
      active_from = q.active_from,
      active_to = q.active_to,
      max_uses_per_code = q.max_uses_per_code,
      max_uses_per_customer = q.max_uses_per_customer,
      created_at = q.created_at,
      archived_at = q.archived_at,
      codes = q.codes
      from (select
              f.id,
              cp.promotion_id,
              cp.form_id,
              context.name as context,
              illuminate_text(f, s, 'name') as name,
              illuminate_text(f, s, 'storefrontName') as storefront_name,
              illuminate_text(f, s, 'description') as description,
              illuminate_text(f, s, 'activeFrom') as active_from,
              illuminate_text(f, s, 'activeTo') as active_to,
              (illuminate_obj(f, s, 'usageRules')->>'usesPerCode') :: integer as max_uses_per_code,
              (illuminate_obj(f, s, 'usageRules')->>'usesPerCustomer') :: integer as max_uses_per_customer,
              to_char(f.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
              to_char(cp.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
              cp.scope as scope,
              jsonb_agg(c.code) filter (where c.code is not null) over (partition by f.id)  as codes
            from coupons as cp
            inner join object_contexts as context on (cp.context_id = context.id)
            left join coupon_codes as c on (cp.form_id = c.coupon_form_id)
            inner join object_forms as f on (f.id = cp.form_id)
            inner join object_shadows as s on (s.id = cp.shadow_id)
            where cp.id = new.id) as q
    where coupons_search_view.id = q.form_id;
    return null;
    end;
$$ language plpgsql;

create or replace function update_coupons_view_update_codes_fn() returns trigger as $$
begin
    update coupons_search_view set
      codes = q.codes
      from (select
              cp.form_id,
              jsonb_agg(c.code) as codes
            from coupons as cp
            left join coupon_codes as c on (cp.form_id = c.coupon_form_id)
            where cp.form_id = (select coupon_form_id from coupon_codes c where c.id = new.id)
            group by cp.form_id) as q
    where coupons_search_view.id = q.form_id;

    return null;
    end;
$$ language plpgsql;
