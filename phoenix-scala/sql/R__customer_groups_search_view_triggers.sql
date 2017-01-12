-- insert customer group function
create or replace function update_customers_groups_view_insert_fn() returns trigger as $$
  begin
    insert into customer_groups_search_view select distinct on (new.id)
      new.id as id,
      new.id as group_id,
      new.name as name,
      new.customers_count as customers_count,
      to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at,
      to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
      new.scope as scope;
    return null;
  end;
$$ language plpgsql;

-- update customer group function
create or replace function update_customers_groups_view_update_fn() returns trigger as $$
  begin
    update customer_groups_search_view set
        name = new.name,
        customers_count = new.customers_count,
        updated_at = to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        scope = new.scope;
    return null;
  end;
$$ language plpgsql;

-- delete customer group function
create or replace function update_customers_groups_view_delete_fn() returns trigger as $$
  begin
    delete from customer_groups_search_view where id = old.id;
    return null;
  end;
$$ language plpgsql;

-- recreate insert customer group trigger
drop trigger if exists update_customers_groups_view_insert_trigger on customer_dynamic_groups;
create trigger update_customers_groups_view_insert_trigger
  after insert on customer_dynamic_groups
  for each row
  execute procedure update_customers_groups_view_insert_fn();

-- recreate update customer group trigger
drop trigger if exists update_customers_groups_view_update_trigger on customer_dynamic_groups;
create trigger update_customers_groups_view_update_trigger
  after update on customer_dynamic_groups
  for each row
  execute procedure update_customers_groups_view_update_fn();

-- recreate delete customer group trigger
drop trigger if exists update_customers_groups_view_delete_trigger on customer_dynamic_groups;
create trigger update_customers_groups_view_delete_trigger
  after delete on customer_dynamic_groups
  for each row
  execute procedure update_customers_groups_view_delete_fn();