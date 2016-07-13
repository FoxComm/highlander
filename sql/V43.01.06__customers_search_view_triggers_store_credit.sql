create or replace function update_customers_view_from_customers_fn() returns trigger as $$
begin
  update customers_search_view set
    store_credit_count = subquery.count,
    store_credit_total = subquery.total
    from (select
            c.id,
            count(sc.id) as count,
            coalesce(sum(sc.available_balance), 0) as total
        from customers as c
        left join store_credits as sc on c.id = sc.customer_id
        group by c.id) as subquery
    where customers_search_view.id = subquery.id;
    return null;
end;
$$ language plpgsql;

create trigger update_customers_view_from_customers
    after insert or update on store_credits
    for each row
    execute procedure update_customers_view_from_customers_fn();
