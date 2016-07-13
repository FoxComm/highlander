create or replace function update_customers_view_from_customers_insert_fn() returns trigger as $$
    begin
        insert into customers_search_view select distinct on (new.id)
            -- customer
            new.id as id,
            new.name as name,
            new.email as email,
            new.is_disabled as is_disabled,
            new.is_guest as is_guest,
            new.is_blacklisted as is_blacklisted,
            new.phone_number as phone_number,
            new.location as location,
            new.blacklisted_by as blacklisted_by,
            new.blacklisted_reason as blacklisted_reason,
            to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as joined_at
            from customers as c;
      return null;
  end;
$$ language plpgsql;

create or replace function update_customers_view_from_customers_update_fn() returns trigger as $$
begin
    update customers_search_view set
        name = new.name,
        email = new.email,
        is_disabled = new.is_disabled,
        is_guest = new.is_guest,
        is_blacklisted = new.is_blacklisted,
        phone_number = new.phone_number,
        location = new.location,
        blacklisted_by = new.blacklisted_by,
        blacklisted_reason = new.blacklisted_reason,        
        joined_at = to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
    where id = NEW.id;
    return null;
    end;
$$ language plpgsql;


create trigger update_customers_view_from_customers_insert
    after insert on customers
    for each row
    execute procedure update_customers_view_from_customers_insert_fn();

create trigger update_customers_view_from_customers_update
    after update on customers
    for each row
    execute procedure update_customers_view_from_customers_update_fn();
