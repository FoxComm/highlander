-- designed for updating customers ranking by scheduled jobs in *_view triggers based tables

create or replace function update_customers_ranking() returns boolean as $$
declare ranking record;
begin

FOR ranking IN
    select c.id,
      c.revenue,
      ntile(100) over (w) as rank
    from
      customers_search_view as c
    where revenue > 0
    window w as (order by c.revenue desc)
    order by revenue desc
  LOOP
  update orders_search_view set
    customer = jsonb_set(customer, '{rank}', jsonb (ranking.rank::varchar), true)
  where customer ->> 'id' = ranking.id::varchar;
END LOOP;


return true;

end;
$$ LANGUAGE plpgsql;