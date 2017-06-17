create or replace function update_failed_auth_view_fn() returns trigger as $$
begin
  insert into failed_authorizations_search_view
    select distinct on (ccc.id)
      -- Credit Card Charge
      ccc.id,
      ccc.stripe_charge_id,
      ccc.amount,
      ccc.currency,
      ccc.state,
      to_char(ccc.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
      -- Credit Card
      cc.holder_name,
      cc.last_four,
      cc.exp_month,
      cc.exp_year,
      cc.brand,
      -- Billing address
      cc.address1,
      cc.address2,
      cc.city,
      cc.zip,
      r.name as region,
      c.name as country,
      c.continent,
      -- Order
      o.reference_number as cord_reference_number,
      -- Customer
      o.account_id as account_id
    from credit_card_charges as ccc
      inner join credit_cards as cc on (ccc.credit_card_id = cc.id)
      inner join regions as r on (cc.region_id = r.id)
      inner join countries as c on (r.country_id = c.id)
      inner join order_payments as op on (op.id = ccc.order_payment_id)
      inner join orders as o on (op.cord_ref = o.reference_number)
    where ccc.state = 'failedAuth' and ccc.id = new.id
    order by ccc.id
  on conflict (id) do nothing ; -- there is immutable values

    return null;
end;
$$ language plpgsql;

drop trigger if exists update_failed_auth_view on credit_card_charges;

create trigger update_failed_auth_view
    after insert or update on credit_card_charges
    for each row
    when (new.state = 'failedAuth')
    execute procedure update_failed_auth_view_fn();
