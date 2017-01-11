-- insert customer group function
create or replace function update_customers_groups_view_insert_fn() returns trigger as $$
  begin
    insert into customer_groups_search_view select distinct on (new.id)
      cg.id as id,
      cg.id as group_id,
      cg.name as name,
      cg.customers_count as customers_count,
      cg.client_state as client_state,
      cg.elastic_request as elastic_request,
      to_char(cg.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at,
      to_char(cg.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
      cg.scope as scope
      from (select
          cdg.id,
          cdg.name,
          cdg.customers_count,
          cdg.client_state,
          cdg.elastic_request,
          cdg.updated_at,
          cdg.created_at,
          cdg.scope
        from customer_dynamic_groups as cdg
        where cdg.id = new.id) as cg;
    return null;
  end;
$$ language plpgsql;

-- update customer group function
create or replace function update_customers_groups_view_update_fn() returns trigger as $$
  begin
    update customer_groups_search_view set
        name = cg.name,
        customers_count = cg.customers_count,
        client_state = cg.client_state,
        elastic_request = cg.elastic_request,
        updated_at = to_char(cg.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        scope = cg.scope
      from (select
          cdg.name,
          cdg.customers_count,
          cdg.client_state,
          cdg.elastic_request,
          cdg.updated_at,
          cdg.scope
        from customer_dynamic_groups as cdg
        where cdg.id = new.id) as cg;
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