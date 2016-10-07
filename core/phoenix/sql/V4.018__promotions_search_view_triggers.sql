-- INSERT
create or replace function update_promotions_search_view_insert_fn() returns trigger as $$
begin
 insert into promotions_search_view select distinct on (p.id)
     f.id,
     context.name as context,
     p.apply_type,
     f.attributes->>(s.attributes->'name'->>'ref') as promotion_name,
     f.attributes->>(s.attributes->'storefrontName'->>'ref') as storefront_name,
     f.attributes->>(s.attributes->'description'->>'ref') as description,
     f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
     f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
     0 as total_used, --this needs to be computed
     0 as current_carts, --this needs to be computed
     to_char(f.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
     to_char(p.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at,
     new.discounts as discounts
   from promotions as p
     inner join object_forms as f on (f.id = p.form_id)
     inner join object_shadows as s on (s.id = p.shadow_id)
     inner join object_contexts as context on (p.context_id = context.id and new.context_id = context.id)
   where new.promotion_id = p.form_id;

return null;
end;
$$ language plpgsql;


create trigger update_promotions_view_insert
    after insert on promotion_discount_links_view
    for each row
    execute procedure update_promotions_search_view_insert_fn();

-- UPDATE

create or replace function update_promotions_view_update_from_self_fn() returns trigger as $$
begin
  update promotions_search_view set
    apply_type = q.apply_type,
    context = q.context,
    promotion_name = q.promotion_name,
    storefront_name = q.storefront_name,
    description = q.description,
    active_from = q.active_from,
    active_to = q.active_to,
    total_used = q.total_used,
    current_carts = q.current_carts,
    created_at = q.created_at,
    archived_at = q.archived_at
      from (select
          p.form_id as promotion_form_id,
          f.id,
          context.name as context,
          p.apply_type,
          f.attributes->>(s.attributes->'name'->>'ref') as promotion_name,
          f.attributes->>(s.attributes->'storefrontName'->>'ref') as storefront_name,
          f.attributes->>(s.attributes->'description'->>'ref') as description,
          f.attributes->>(s.attributes->'activeFrom'->>'ref') as active_from,
          f.attributes->>(s.attributes->'activeTo'->>'ref') as active_to,
          0 as total_used, --this needs to be computed
          0 as current_carts, --this needs to be computed
          to_char(f.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
          to_char(p.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at
        from promotions as p
        inner join object_forms as f on (f.id = p.form_id)
        inner join object_shadows as s on (s.id = p.shadow_id)
        inner join object_contexts as context on (p.context_id = context.id)
      where new.id = p.id) as q
    where promotions_search_view.id = q.promotion_form_id;


  return null;
end;
$$ language plpgsql;

create or replace function update_promotions_view_update_from_disc_fn() returns trigger as $$
begin
  update promotions_search_view set
    discounts = new.discounts
  where id = new.promotion_id;

  return null;
end;
$$ language plpgsql;

create trigger update_promotions_view_update
  after update on promotions
  for each row
  execute procedure update_promotions_view_update_from_self_fn();

create trigger update_promotions_view_from_discounts
  after update on promotion_discount_links_view
  for each row
  execute procedure update_promotions_view_update_from_disc_fn();

create or replace function update_promotions_search_view_totals_fn() returns trigger as $$
begin
  update promotions_search_view set
    total_used = q.sum_total_used from (select
        sum(c.total_used) as sum_total_used
      from coupons_search_view as c
      where c.promotion_id = new.promotion_id
      group by c.promotion_id) as q
    where promotions_search_view.id = new.promotion_id;

  return null;
end;
$$ language plpgsql;

create trigger update_promotions_search_view_totals
  after update on coupons_search_view
  for each row
  when (old.total_used is distinct from new.total_used)
  execute procedure update_promotions_search_view_totals_fn();
