-- insert customer group function
create or replace function update_customers_groups_view_insert_fn() returns trigger as $$
  begin
    insert into customer_groups_search_view (
      id,
      group_id,
      name,
      group_type,
      customers_count,
      scope,
      created_at,
      updated_at,
      deleted_at
    ) select distinct on (new.id)
        new.id as id,
        new.id as group_id,
        new.name as name,
        new.group_type as group_type,
        new.customers_count as customers_count,
        new.scope as scope,
        to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
        to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at,
        to_char(new.deleted_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as deleted_at;
    return null;
  end;
$$ language plpgsql;

-- update customer group function
create or replace function update_customers_groups_view_update_fn() returns trigger as $$
  begin
    update customer_groups_search_view set
        name = new.name,
        group_type = new.group_type,
        customers_count = new.customers_count,
        updated_at = to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        deleted_at = to_char(new.deleted_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        scope = new.scope
    where id = new.id;
    return null;
  end;
$$ language plpgsql;

-- delete customer group function
drop function if exists update_customers_groups_view_delete_fn();

-- recreate insert customer group trigger
drop trigger if exists update_customers_groups_view_insert_trigger on customer_groups;
create trigger update_customers_groups_view_insert_trigger
  after insert on customer_groups
  for each row
  execute procedure update_customers_groups_view_insert_fn();

-- recreate update customer group trigger
drop trigger if exists update_customers_groups_view_update_trigger on customer_groups;
create trigger update_customers_groups_view_update_trigger
  after update on customer_groups
  for each row
  execute procedure update_customers_groups_view_update_fn();

-- recreate delete customer group trigger
drop trigger if exists update_customers_groups_view_delete_trigger on customer_groups;
