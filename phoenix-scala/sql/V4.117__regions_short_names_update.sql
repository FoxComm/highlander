-- we're need to fix this function here (defined in R__customers_search_view_triggers.sql) because it will be triggered by following updates

create or replace function update_customers_view_from_shipping_addresses_fn() returns trigger as $$
declare account_ids integer[];
begin
  case tg_table_name
    when 'addresses' then
      account_ids := array_agg(new.account_id);
    when 'regions' then
      select array_agg(a.account_id) into strict account_ids
      from addresses as a
      inner join regions as r on (r.id = a.region_id)
      where r.id = new.id;
    when 'countries' then
      select array_agg(a.account_id) into strict account_ids
      from addresses as a
      inner join regions as r1 on (r1.id = a.region_id)
      inner join countries as c1 on (r1.country_id = c1.id)
      where c1.id = new.id;
  end case;

  update customers_search_view set
    shipping_addresses_count = subquery.count,
    shipping_addresses = subquery.addresses
    from (select
            c.account_id as id,
            count(a) as count,
            case when count(a) = 0
            then
                '[]'
            else
                json_agg((
                  a.address1,
                  a.address2,
                  a.city,
                  a.zip,
                  r1.name,
                  c1.name,
                  c1.continent,
                  c1.currency
                )::export_addresses)::jsonb
            end as addresses
        from customer_data as c
        left join addresses as a on (a.account_id = c.account_id)
        left join regions as r1 on (a.region_id = r1.id)
        left join countries as c1 on (r1.country_id = c1.id)
        where c.account_id = any(account_ids)
        group by c.account_id) as subquery
    where customers_search_view.id = subquery.id;

    return null;
end;
$$ language plpgsql;

update regions set abbreviation = 'AL' where name = 'Alabama';
update regions set abbreviation = 'AK' where name = 'Alaska';
update regions set abbreviation = 'AZ' where name = 'Arizona';
update regions set abbreviation = 'AR' where name = 'Arkansas';
update regions set abbreviation = 'CA' where name = 'California';
update regions set abbreviation = 'CO' where name = 'Colorado';
update regions set abbreviation = 'CT' where name = 'Connecticut';
update regions set abbreviation = 'DE' where name = 'Delaware';
update regions set abbreviation = 'FL' where name = 'Florida';
update regions set abbreviation = 'GA' where name = 'Georgia';
update regions set abbreviation = 'HI' where name = 'Hawaii';
update regions set abbreviation = 'ID' where name = 'Idaho';
update regions set abbreviation = 'IL' where name = 'Illinois';
update regions set abbreviation = 'IN' where name = 'Indiana';
update regions set abbreviation = 'IA' where name = 'Iowa';
update regions set abbreviation = 'KS' where name = 'Kansas';
update regions set abbreviation = 'KY' where name = 'Kentucky';
update regions set abbreviation = 'LA' where name = 'Louisiana';
update regions set abbreviation = 'ME' where name = 'Maine';
update regions set abbreviation = 'MD' where name = 'Maryland';
update regions set abbreviation = 'MA' where name = 'Massachusetts';
update regions set abbreviation = 'MI' where name = 'Michigan';
update regions set abbreviation = 'MN' where name = 'Minnesota';
update regions set abbreviation = 'MS' where name = 'Mississippi';
update regions set abbreviation = 'MO' where name = 'Missouri';
update regions set abbreviation = 'MT' where name = 'Montana';
update regions set abbreviation = 'NE' where name = 'Nebraska';
update regions set abbreviation = 'NV' where name = 'Nevada';
update regions set abbreviation = 'NH' where name = 'New Hampshire';
update regions set abbreviation = 'NJ' where name = 'New Jersey';
update regions set abbreviation = 'NM' where name = 'New Mexico';
update regions set abbreviation = 'NY' where name = 'New York';
update regions set abbreviation = 'NC' where name = 'North Carolina';
update regions set abbreviation = 'ND' where name = 'North Dakota';
update regions set abbreviation = 'OH' where name = 'Ohio';
update regions set abbreviation = 'OK' where name = 'Oklahoma';
update regions set abbreviation = 'OR' where name = 'Oregon';
update regions set abbreviation = 'PA' where name = 'Pennsylvania';
update regions set abbreviation = 'RI' where name = 'Rhode Island';
update regions set abbreviation = 'SC' where name = 'South Carolina';
update regions set abbreviation = 'SD' where name = 'South Dakota';
update regions set abbreviation = 'TN' where name = 'Tennessee';
update regions set abbreviation = 'TX' where name = 'Texas';
update regions set abbreviation = 'UT' where name = 'Utah';
update regions set abbreviation = 'VT' where name = 'Vermont';
update regions set abbreviation = 'VA' where name = 'Virginia';
update regions set abbreviation = 'WA' where name = 'Washington';
update regions set abbreviation = 'WV' where name = 'West Virginia';
update regions set abbreviation = 'WI' where name = 'Wisconsin';
update regions set abbreviation = 'WY' where name = 'Wyoming';
update regions set abbreviation = 'DC' where name = 'Washington DC';
update regions set abbreviation = 'PR' where name = 'Puerto Rico';
update regions set abbreviation = 'VI' where name = 'Virgin Islands, U.S.';
update regions set abbreviation = 'AS' where name = 'American Samoa';
update regions set abbreviation = 'GU' where name = 'Guam';
update regions set abbreviation = 'MP' where name = 'Northern Mariana Islands';
update regions set abbreviation = 'AA' where name = 'Armed Forces Americas';
update regions set abbreviation = 'AE' where name = 'Armed Forces Europe';
update regions set abbreviation = 'AP' where name = 'Armed Forces Pacific';
update regions set abbreviation = 'DC' where name = 'District of Columbia';
