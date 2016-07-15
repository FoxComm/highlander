create table store_admins_search_view
(
    id bigint not null unique,
    email email not null,
    name generic_string,
    phone_number phone_number,
    department generic_string,
    state generic_string,
    created_at text
);

create or replace function update_store_admins_view_insert_fn() returns trigger as $$
    begin
        insert into store_admins_search_view select distinct on (new.id)
            -- customer
            new.id as id,
            new.name as name,
            new.email as email,
            new.phone_number as phone_number,
            new.department as department,
            new.state as state,
            to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at
            from customers as c;
      return null;
  end;
$$ language plpgsql;

create or replace function update_store_admins_view_update_fn() returns trigger as $$
begin
    update store_admins_search_view set
        name = new.name,
        email = new.email,
        phone_number = new.phone_number,
        department = new.department,
        state = new.state,      
        created_at = to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
    where id = new.id;
    return null;
    end;
$$ language plpgsql;

create trigger update_store_admins_view_insert
    after insert on store_admins
    for each row
    execute procedure update_store_admins_view_insert_fn();

create trigger update_store_admins_view_update
    after update on store_admins
    for each row
    execute procedure update_store_admins_view_update_fn();
