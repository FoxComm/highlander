alter table carts add column scope text not null;
alter table carts_search_view add column scope text not null;

update carts set scope = exts.text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);
update carts_search_view set scope = text2ltree(get_scope_path((select scope_id from organizations where name = 'merchant'))::text);

create or replace function update_carts_view_from_carts_insert_fn() returns trigger as $$
begin
  insert into carts_search_view(
    id,
    reference_number,
    created_at,
    updated_at,
    currency,
    sub_total,
    shipping_total,
    adjustments_total,
    taxes_total,
    grand_total,
    customer,
    scope) select distinct on (new.id)
                                  -- order
                                  new.id as id,
                                  new.reference_number as reference_number,
                                  to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
                                  to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at,
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
                                  )::jsonb as customer,
                                  new.scope as scope
                                from customers_search_view as c
                                where (new.account_id = c.id);
  return null;
end;
$$ language plpgsql;
