create or replace function update_orders_view_from_orders_insert_fn() returns trigger as $$
    begin
        insert into orders_search_view (
            id,
            scope,
            reference_number,
            state,
            placed_at,
            currency,
            sub_total,
            shipping_total,
            adjustments_total,
            taxes_total,
            grand_total,
            customer)
        select distinct on (new.id)
            nextval('orders_search_view_id_seq') as id,
            -- order
            new.scope as scope,
            new.reference_number as reference_number,
            new.state as state,
            to_json_timestamp(new.placed_at) as placed_at,
            new.currency as currency,
            -- totals
            new.sub_total as sub_total,
            new.shipping_total as shipping_total,
            new.adjustments_total as adjustments_total,
            new.taxes_total as taxes_total,
            new.grand_total as grand_total,
            -- customer
            json_build_object(
                'id', c.id,
                'name', c.name,
                'email', c.email,
                'is_blacklisted', c.is_blacklisted,
                'joined_at', c.joined_at,
                'rank', c.rank,
                'revenue', c.revenue
            )::jsonb as customer
        from customers_search_view as c
        where (new.account_id = c.id);
        return null;
    end;
$$ language plpgsql;

create or replace function update_orders_view_from_line_items_fn() returns trigger as $$
declare affected_cord_ref text;
begin
  case tg_table_name
    when 'order_line_items' then
      affected_cord_ref := new.cord_ref;
    when 'orders_search_view' then
      affected_cord_ref := new.reference_number;
  end case;
  update orders_search_view set
    line_item_count = subquery.count,
    line_items = subquery.items from (select
          o.id,
          count(sku.id) as count,
          case when count(sku) = 0
          then
            '[]'
          else
            json_agg((
                    oli.reference_number,
                    oli.state,
                    sku.code,
                    sku_form.attributes->>(sku_shadow.attributes->'title'->>'ref'),
                    sku_form.attributes->>(sku_shadow.attributes->'externalId'->>'ref'),
                    sku_form.attributes->(sku_shadow.attributes->'salePrice'->>'ref')->>'value',
                    oli.attributes,
                    sku.scope)::export_line_items)
                    ::jsonb
          end as items
          from orders as o
          left join order_line_items as oli on (o.reference_number = oli.cord_ref)
          left join skus as sku on (oli.sku_id = sku.id)
          left join object_forms as sku_form on (sku.form_id = sku_form.id)
          left join object_shadows as sku_shadow on (oli.sku_shadow_id = sku_shadow.id)
          where o.reference_number = affected_cord_ref
          group by o.id) as subquery
      where orders_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

create or replace function update_orders_view_from_orders_update_fn() returns trigger as $$
begin
    update orders_search_view set
        state = new.state,
        placed_at = to_char(new.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        currency = new.currency,
        sub_total = new.sub_total,
        shipping_total = new.shipping_total,
        adjustments_total = new.adjustments_total,
        taxes_total = new.taxes_total,
        grand_total = new.grand_total
    where reference_number = new.reference_number;

    return null;
end;
$$ language plpgsql;

create or replace function update_orders_view_from_shipping_addresses_fn() returns trigger as $$
declare cord_refs text[];
begin
  case tg_table_name
    when 'addresses' then
    select array_agg(ac.cord_ref) into strict cord_refs
    from address_cord as ac where ac.address_id = new.id;
    when 'regions' then
    select array_agg(ac.cord_ref) into strict cord_refs
    from addresses as osa
      inner join regions as r on (r.id = osa.region_id)
      inner join address_cord as ac on osa.id = ac.address_id
    where r.id = new.id;
    when 'countries' then
    select array_agg(ac.cord_ref) into strict cord_refs
    from addresses as osa
      inner join regions as r1 on (r1.id = osa.region_id)
      inner join countries as c1 on (r1.country_id = c1.id)
      inner join address_cord as ac on osa.id = ac.address_id
    where c1.id = new.id;
  end case;

  update orders_search_view set
    shipping_addresses_count = subquery.count,
    shipping_addresses = subquery.addresses
  from (select
          o.id,
          count(osa) as count,
          case when count(osa) = 0
            then
              '[]'
          else
            json_agg((
                       osa.address1,
                       osa.address2,
                       osa.city,
                       osa.zip,
                       r1.name,
                       c1.name,
                       c1.continent,
                       c1.currency
                     )::export_addresses)::jsonb
          end as addresses
        from orders as o
          left join address_cord as ac on ac.cord_ref = o.reference_number
          left join addresses as osa on (o.reference_number = ac.cord_ref)
          left join regions as r1 on (osa.region_id = r1.id)
          left join countries as c1 on (r1.country_id = c1.id)
        where o.reference_number = any(cord_refs)
        group by o.id) as subquery
  where orders_search_view.id = subquery.id;

  return null;
end;
$$ language plpgsql;

drop trigger if exists update_orders_view_from_order_shipping_addresses on addresses;
create trigger update_orders_view_from_addresses
after update or insert on addresses
for each row
execute procedure update_orders_view_from_shipping_addresses_fn();

drop trigger if exists existsupdate_orders_view_from_regions on regions;
create trigger update_orders_view_from_regions
after update or insert on regions
for each row
execute procedure update_orders_view_from_shipping_addresses_fn();

drop trigger if exists update_orders_view_from_countries on countries;
create trigger update_orders_view_from_countries
after update or insert on countries
for each row
execute procedure update_orders_view_from_shipping_addresses_fn();
