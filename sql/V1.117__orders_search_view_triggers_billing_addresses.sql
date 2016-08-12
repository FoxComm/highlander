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

create trigger update_orders_view_for_billing_addresses
    after insert on orders
    for each row
    execute procedure update_orders_view_from_billing_addresses_fn();
