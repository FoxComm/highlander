create or replace function update_orders_view_from_orders_insert_fn() returns trigger as $$
    begin
        insert into orders_search_view select distinct on (new.id)
            -- order
            new.id as id,
            new.reference_number as reference_number,
            new.state as state,
            to_char(new.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as placed_at,
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

create or replace function update_orders_view_from_orders_update_fn() returns trigger as $$
begin
    update orders_search_view set
        reference_number = new.reference_number,
        state = new.state,
        placed_at = to_char(new.placed_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        currency = new.currency,
        sub_total = new.sub_total,
        shipping_total = new.shipping_total,
        adjustments_total = new.adjustments_total,
        taxes_total = new.taxes_total,
        grand_total = new.grand_total
    where id = new.id;

    return null;
end;
$$ language plpgsql;


create or replace function update_orders_view_from_billing_addresses_fn() returns trigger as $$
begin
  update orders_search_view set
    billing_addresses_count = subquery.count,
    billing_addresses = subquery.addresses
    from (select
            o.id,
            count(cc) as count,
            case when count(cc) = 0
            then
                '[]'
            else
                json_agg((
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
        from orders as o
        left join order_payments as op_cc on (o.reference_number = op_cc.cord_ref and op_cc.payment_method_type = 'creditCard')
        left join credit_cards as cc on (cc.id = op_cc.payment_method_id)
        left join regions as r2 on (cc.region_id = r2.id)
        left join countries as c2 on (r2.country_id = c2.id)
        where o.id = new.id
        group by o.id) as subquery
    where orders_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;
