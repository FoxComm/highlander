drop materialized view coupons_search_view;

create table coupons_search_view
(
    id bigint not null,
    promotion_id bigint,
    context generic_string,
    name generic_string,
    storefront_name generic_string,
    description text,
    active_from json_timestamp,
    active_to json_timestamp,
    total_used integer,
    created_at json_timestamp not null,
    archived_at json_timestamp
);

create unique index coupons_search_view_id_idx on coupons_search_view (id);

create or replace function update_coupons_view_insert_fn() returns trigger as $$
  begin
    insert into coupons_search_view select distinct on (new.id)
      f.id,
      cp.promotion_id,
      context.name as context,
      f.attributes->>(s.attributes->'name'->>'ref') as name,
      f.attributes->>(s.attributes->'storefrontName'->>'ref') as storefront_name,
      f.attributes->>(s.attributes->'description'->>'ref') as description,
      f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
      f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
      0 as total_used,
      to_char(f.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
      to_char(cp.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at
      from coupons as cp
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
      created_at = q.created_at,
      archived_at = q.archived_at
      from (select
              f.id,
              cp.promotion_id,
              cp.form_id,
              context.name as context,
              f.attributes->>(s.attributes->'name'->>'ref') as name,
              f.attributes->>(s.attributes->'storefrontName'->>'ref') as storefront_name,
              f.attributes->>(s.attributes->'description'->>'ref') as description,
              f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
              f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
              to_char(f.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
              to_char(cp.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at
            from coupons as cp
            inner join object_contexts as context on (cp.context_id = context.id)
            inner join object_forms as f on (f.id = cp.form_id)
            inner join object_shadows as s on (s.id = cp.shadow_id)
            where cp.id = new.id) as q
    where coupons_search_view.id = q.form_id;
    return null;
    end;
$$ language plpgsql;

create or replace function update_coupons_view_totals_fn() returns trigger as $$
begin
  update coupons_search_view set
    total_used = new.count
    where coupons_search_view.id = new.coupon_form_id;

  return null;
end;
$$ language plpgsql;


create trigger update_coupons_view_insert
    after insert on coupons
    for each row
    execute procedure update_coupons_view_insert_fn();

create trigger update_coupons_view_update
  after update on coupons
  for each row
  execute procedure update_coupons_view_update_fn();

create trigger update_coupons_view_totals
  after update on coupon_usages
  for each row
  execute procedure update_coupons_view_totals_fn();
