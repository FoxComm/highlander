-- Procedures for updating customers_search_view

-- Update search view after new customer inserted

create or replace function update_customers_view_from_customers_insert_fn() returns trigger as $$
    begin
        insert into customers_search_view (id, name, email, is_disabled, is_guest, is_blacklisted, phone_number, blacklisted_by, joined_at, scope) select
            -- customer
            new.account_id as id,
            u.name as name,
            u.email as email,
            u.is_disabled as is_disabled,
            new.is_guest as is_guest,
            u.is_blacklisted as is_blacklisted,
            u.phone_number as phone_number,
            u.blacklisted_by as blacklisted_by,
            to_char(u.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as joined_at,
            c.scope as scope
            from customer_data as c, users as u
            where c.account_id = new.account_id and u.account_id = new.account_id;

      return null;
  end;
$$ language plpgsql;

-- Update search view after customer updated

create or replace function update_customers_view_from_customers_update_fn() returns trigger as $$
begin
    update customers_search_view set
        name = u.name,
        email = u.email,
        is_disabled = u.is_disabled,
        is_guest = cu.is_guest,
        is_blacklisted = u.is_blacklisted,
        phone_number = u.phone_number,
        blacklisted_by = u.blacklisted_by,
        joined_at = to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
    from users as u, customer_data as cu
    where customers_search_view.id = new.account_id and u.account_id = new.account_id
    and cu.account_id = new.account_id;

    return null;
    end;
$$ language plpgsql;

drop trigger if exists update_customers_view_from_customers_data_update on customer_data;
drop trigger if exists update_customers_view_from_users_update on users;

create trigger update_customers_view_from_customers_data_update
    after update on customer_data
    for each row
    execute procedure update_customers_view_from_customers_update_fn();

create trigger update_customers_view_from_users_update
    after update on users
    for each row
    execute procedure update_customers_view_from_customers_update_fn();


-- Update customer's shipping address after address/region/country inserted or updated

create or replace function update_customers_view_from_shipping_addresses_fn() returns trigger as $$
declare account_ids integer[];
begin
  case tg_table_name
    when 'addresses' then
      account_ids := array_agg(new.account_id);
    when 'regions' then
      select array_agg(o.account_id) into strict account_ids
      from addresses as a
      inner join regions as r on (r.id = a.region_id)
      where r.id = new.id;
    when 'countries' then
      select array_agg(o.account_id) into strict account_ids
      from addresses as a
      inner join regions as r1 on (r1.id = a.region_id)
      inner join countries as c1 on (r1.country_id = c1.id)
      where c1.id = new.id;
  end case;

  update customers_search_view set
    shipping_addresses_count = subquery.count,
    shipping_addresses = subquery.addresses
    from (select
            c.account_id as id,
            count(a) as count,
            case when count(a) = 0
            then
                '[]'
            else
                json_agg((
                  a.address1,
                  a.address2,
                  a.city,
                  a.zip,
                  r1.name,
                  c1.name,
                  c1.continent,
                  c1.currency
                )::export_addresses)::jsonb
            end as addresses
        from customer_data as c
        left join addresses as a on (a.account_id = c.account_id)
        left join regions as r1 on (a.region_id = r1.id)
        left join countries as c1 on (r1.country_id = c1.id)
        where c.account_id = any(account_ids)
        group by c.account_id) as subquery
    where customers_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

-- Update customer's billing address after credit card/region/country updated or inserted

create or replace function update_customers_view_from_billing_addresses_fn() returns trigger as $$
declare account_ids integer[];
begin
  case tg_table_name
    when 'credit_cards' then
      account_ids := array_agg(new.account_id);
    when 'regions' then
      select array_agg(cc.account_id) into strict account_ids
      from credit_cards as cc
      inner join regions as r on (cc.region_id = r.id)
      where r.id = new.id;
    when 'countries' then
      select array_agg(o.account_id) into strict account_ids
      from credit_cards as cc
      inner join regions as r on (cc.region_id = r.id)
      inner join countries as c on (c.id = r.country_id)
      where c.id = new.id;
  end case;

  update customers_search_view set
    billing_addresses_count = subquery.count,
    billing_addresses = subquery.addresses
    from (select
            c.account_id as id,
            count(distinct (
                cc.address1,
                cc.address2,
                cc.city,
                cc.zip
            )) as count,
            case when count(cc) = 0
            then
                '[]'
            else
                json_agg(distinct (
                  cc.address1,
                  cc.address2,
                  cc.city,
                  cc.zip,
                  r2.name,
                  c2.name,
                  c2.continent,
                  c2.currency
                )::export_addresses)::jsonb
            end as addresses
        from customer_data as c
        left join credit_cards as cc on (cc.account_id = c.account_id)
        left join regions as r2 on (cc.region_id = r2.id)
        left join countries as c2 on (r2.country_id = c2.id)
        where c.account_id = any(account_ids) and in_wallet = true
        group by c.account_id) as subquery
    where customers_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

-- Update customer's orders after order inserted or updated

create or replace function update_customers_view_from_orders_fn() returns trigger as $$
begin
    update customers_search_view set
        order_count = subquery.order_count,
        orders = subquery.orders
        from (select
                c.account_id as id,
                count(o.id) as order_count,
                case when count(o) = 0
                  then
                    '[]'
                else
                  json_agg((
                    o.account_id,
                    o.reference_number,
                    o.state,
                    to_char(o.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
                    o.sub_total,
                    o.shipping_total,
                    o.adjustments_total,
                    o.taxes_total,
                    o.grand_total,
                    0 -- FIXME
                  )::export_orders)::jsonb
                end as orders
              from customer_data as c
              left join orders as o on (c.account_id = o.account_id)
              where c.account_id = new.account_id
              group by c.account_id) as subquery
    where customers_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

-- Update search view after store credits inserted or updated

create or replace function update_customers_view_from_customers_fn() returns trigger as $$
begin
  update customers_search_view set
    store_credit_count = subquery.count,
    store_credit_total = subquery.total
    from (select
            c.account_id as id,
            count(sc.id) as count,
            coalesce(sum(sc.available_balance), 0) as total
        from customer_data as c
        left join store_credits as sc on c.account_id = sc.account_id
        group by c.account_id) as subquery
    where customers_search_view.id = subquery.id;
    return null;
end;
$$ language plpgsql;

-- Update customer's revenue value after order got shipped

create or replace function update_customers_view_revenue_fn() returns trigger as $$
begin
    update customers_search_view set
        revenue = subquery.revenue
        from (
			select
				c.account_id as id,
		    	coalesce(sum(ccc.amount), 0) + coalesce(sum(sca.debit), 0) + coalesce(sum(gca.debit), 0) as revenue
		    from customer_data as c
		    inner join orders on (c.account_id = orders.account_id and orders.state = 'shipped')
		    inner join order_payments as op on (op.cord_ref = orders.reference_number)
		    left join credit_card_charges as ccc on (ccc.order_payment_id = op.id and ccc.state = 'fullCapture')
		    left join store_credit_adjustments as sca on (sca.order_payment_id = op.id and sca.state = 'capture')
		    left join gift_card_adjustments as gca on (gca.order_payment_id = op.id and gca.state = 'capture')
		    where is_guest = false and c.account_id = new.account_id
		    group by c.account_id
		    order by c.account_id) as subquery
    where customers_search_view.id = subquery.id;
    return null;
end;
$$ language plpgsql;

-- Update customer's rank

create or replace function update_customers_ranking() returns boolean as $$
begin
	 -- Update customers ranks
      update customers_search_view
        set rank = q.rank from (
            select
              c.id,
              c.revenue,
              ntile(100) over (w) as rank
            from
              customers_search_view as c
            where revenue > 0
              window w as (order by c.revenue desc)
              order by revenue desc) as q
          where customers_search_view.id = q.id;

      -- Update Carts
      update carts_search_view set
        customer = q.customer from (
          select
              cs.id,
              c.rank,
              jsonb_set(jsonb_set(cs.customer, '{rank}', jsonb (c.rank::varchar), true),
                        '{revenue}', jsonb (c.revenue::varchar), true)
                        as customer
          from carts_search_view as cs
          inner join customers_search_view as c on (c.id = (cs.customer->>'id')::bigint)
          where c.rank > 0
        ) as q
        where carts_search_view.id = q.id;

      -- Update Orders
      update orders_search_view set
        customer = q.customer from (
          select
              o.id,
              c.rank,
              jsonb_set(jsonb_set(o.customer, '{rank}', jsonb (c.rank::varchar), true),
                        '{revenue}', jsonb (c.revenue::varchar), true)
                      as customer
          from orders_search_view as o
          inner join customers_search_view as c on (c.id = (o.customer->>'id')::bigint)
          where c.rank > 0
        ) as q
        where orders_search_view.id = q.id;

	return true;
end;
$$ language plpgsql;
