-- set sequence value as max id + 1
create sequence if not exists  orders_search_view_id_seq increment by 1;

select setval ('orders_search_view_id_seq',
               coalesce((select max (id) + 1 from orders_search_view), 1), false);

alter table orders_search_view
   alter column id set default nextval ('orders_search_view_id_seq');

-- from amazon orders
create or replace function update_orders_search_view_from_amazon_orders_insert_fn() returns trigger as $$
begin
  insert into orders_search_view (
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
    -- order
    new.scope as scope,
    new.amazon_order_id as reference_number,
    new.order_status as state,
    to_char(new.purchase_date, 'yyyy-mm-dd"t"hh24:mi:ss.ms"z"') as placed_at,
    new.currency as currency,
    -- totals
    0 as sub_total,
    0 as shipping_total,
    0 as adjustments_total,
    0 as taxes_total,
    new.order_total as grand_total,
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

-- from orders

create or replace function update_orders_view_from_orders_insert_fn() returns trigger as $$
begin
  insert into orders_search_view (
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
    -- order
    new.scope as scope,
    new.reference_number as reference_number,
    new.state as state,
    to_char(new.placed_at, 'yyyy-mm-dd"t"hh24:mi:ss.ms"z"') as placed_at,
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

--

create or replace function update_orders_view_from_orders_insert_fn() returns trigger as $$
begin
  insert into orders_search_view select distinct on (new.id)
    -- order
    new.reference_number as reference_number,
    new.state as state,
    to_char(new.placed_at, 'yyyy-mm-dd"t"hh24:mi:ss.ms"z"') as placed_at,
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
      'joined_at', to_char(c.created_at, 'yyyy-mm-dd"t"hh24:mi:ss.ms"z"'),
      'rank', rank.rank,
      'revenue', coalesce(rank.revenue, 0)
    )::jsonb as customer
    from customers as c
    left join customers_ranking as rank on (c.id = rank.id)
    where (new.customer_id = c.id);
  return null;
end;
$$ language plpgsql;

--

create or replace function update_orders_view_from_orders_insert_fn() returns trigger as $$
begin
  insert into orders_search_view select distinct on (new.id)
    -- order
    new.reference_number as reference_number,
    new.state as state,
    to_char(new.placed_at, 'yyyy-mm-dd"t"hh24:mi:ss.ms"z"') as placed_at,
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
    where (new.customer_id = c.id);
  return null;
end;
$$ language plpgsql;

--

create or replace function update_orders_view_from_orders_insert_fn() returns trigger as $$
begin
  insert into orders_search_view select distinct on (new.id)
    -- order
    new.reference_number as reference_number,
    new.state as state,
    to_char(new.placed_at, 'yyyy-mm-dd"t"hh24:mi:ss.ms"z"') as placed_at,
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

--

create or replace function update_orders_view_from_orders_insert_fn() returns trigger as $$
begin
  insert into orders_search_view (
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
    -- order
    new.scope as scope,
    new.reference_number as reference_number,
    new.state as state,
    to_char(new.placed_at, 'yyyy-mm-dd"t"hh24:mi:ss.ms"z"') as placed_at,
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