create or replace function update_orders_search_view_from_amazon_orders_insert_fn() returns trigger as $$
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
            -- order
            ((select max(id) from orders_search_view group by id order by id DESC limit 1) + 1) as id,
            new.scope as scope,
            new.amazon_order_id as reference_number,
            new.order_status as state,
            to_char(new.purchase_date, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as placed_at,
            new.currency as currency,
            -- totals
            0 as sub_total,
            0 as shipping_total,
            0 as adjustments_total,
            0 as taxes_total,
            new.order_total as grand_total,
            -- customer
            json_build_object(
                'id', 0,
                'name', ao.customer_name,
                'email', ao.customer_email,
                'is_blacklisted', false,
                'joined_at', null,
                'rank', null,
                'revenue', null
            )::jsonb as customer
        from amazon_orders as ao
        where (new.amazon_order_id = ao.amazon_order_id);
        return null;
    end;
$$ language plpgsql;

-- update customer group function
create or replace function update_orders_search_view_from_amazon_orders_update_fn() returns trigger as $$
  begin
    update orders_search_view set
        state = new.order_status
    where reference_number = new.amazon_order_id;
    return null;
  end;
$$ language plpgsql;

drop trigger if exists update_orders_search_view_from_amazon_orders_insert_trigger on amazon_orders;
create trigger update_orders_search_view_from_amazon_orders_insert_trigger
  after insert on amazon_orders
  for each row
  execute procedure update_orders_search_view_from_amazon_orders_insert_fn();

drop trigger if exists update_orders_search_view_from_amazon_orders_update_trigger on amazon_orders;
create trigger update_orders_search_view_from_amazon_orders_update_trigger
  after update on amazon_orders
  for each row
  execute procedure update_orders_search_view_from_amazon_orders_update_fn();

