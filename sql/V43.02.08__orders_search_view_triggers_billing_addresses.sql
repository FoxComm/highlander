create or replace function update_orders_view_from_billing_addresses_fn() returns trigger as $$
declare cord_refs text[];
begin
  case TG_TABLE_NAME
    when 'order_payments' then
      cord_refs := array_agg(new.cord_ref);
    when 'credit_cards' then
      select array_agg(op.cord_ref) into strict cord_refs
      from credit_cards as cc
      inner join order_payments as op on (cc.id = op.payment_method_id)
      where cc.id = new.id;
    when 'regions' then
      select array_agg(op.cord_ref) into strict cord_refs
      from credit_cards as cc
      inner join order_payments as op on (cc.id = op.payment_method_id)
      inner join regions as r on (cc.region_id = r.id)
      where r.id = new.id;
    when 'countries' then
      select array_agg(op.cord_ref) into strict cord_refs
      from credit_cards as cc
      inner join order_payments as op on (cc.id = op.payment_method_id)
      inner join regions as r on (cc.region_id = r.id)
      inner join countries as c on (c.id = r.country_id)
      where c.id = new.id;
  end case;

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
                json_agg((cc.address1, cc.address2, cc.city, cc.zip, r2.name, c2.name, c2.continent, c2.currency)
                ::export_addresses)::jsonb
            end as addresses
        from orders as o
        left join order_payments as op_cc on (o.reference_number = op_cc.cord_ref and op_cc.payment_method_type = 'creditCard')
        left join credit_cards as cc on (cc.id = op_cc.payment_method_id)
        left join regions as r2 on (cc.region_id = r2.id)
        left join countries as c2 on (r2.country_id = c2.id)
        where o.reference_number = any(cord_refs)
        group by o.id) as subquery
    where orders_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;


create trigger update_orders_view_from_billing_addresses_order_payments
    after update or insert on order_payments
    for each row
    when (new.payment_method_type = 'creditCard')
    execute procedure update_orders_view_from_billing_addresses_fn();

create trigger update_orders_view_from_billing_addresses_credit_cards
    after update or insert on credit_cards
    for each row
    execute procedure update_orders_view_from_billing_addresses_fn();


create trigger update_orders_view_from_billing_addresses_regions
    after update or insert on regions
    for each row
    execute procedure update_orders_view_from_billing_addresses_fn();

create trigger update_orders_view_from_billing_addresses_countries
    after update or insert on countries
    for each row
    execute procedure update_orders_view_from_billing_addresses_fn();

