alter table customers_search_view add column revenue bigint not null;

create or replace function update_customers_view_from_revenue_fn() returns trigger as $$
begin
    update customers_search_view set 
        revenue = subquery.revenue 
        from (
            select coalesce(cm.revenue, 0) as revenue
            from customers as c
            left join customers_ranking as cm on (c.id = cm.id)
            where c.id = new.id) as subquery
    where customers_search_view.id = new.id;
    return null;
end;
$$ language plpgsql;

create trigger update_customers_view_from_revenue
    after insert or update on customers
    for each row
    execute procedure update_customers_view_from_revenue_fn();
