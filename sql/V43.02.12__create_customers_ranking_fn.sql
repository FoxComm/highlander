-- designed for updating customers ranking by scheduled jobs in *_view triggers based tables
-- TODO: update to jsonb_set when 9.5 will be available
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
    customer =
    -- TODO: uncomment when jsonb_set in 9.5 will be available
    -- jsonb_set(customer, '{rank}', jsonb (ranking.rank::varchar), true)
    json_build_object(
           'id', customer ->> 'id',
           'name', customer ->> 'name',
           'email', customer ->> 'email',
           'is_blacklisted', customer ->> 'is_blacklisted',
           'joined_at', customer ->> 'joined_at',
           'rank', ranking.rank,
           'revenue', customer ->> 'revenue'
       )::jsonb
  where customer ->> 'id' = ranking.id::varchar;
END LOOP;


return true;

end;
$$ LANGUAGE plpgsql;