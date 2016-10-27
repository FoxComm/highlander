create or replace function update_store_admins_view_update_fn() returns trigger as $$
begin
    update store_admins_search_view set
        name = u.name,
        email = u.email,
        phone_number = u.phone_number,
        state = a.state,
        created_at = to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
    from users as u, admin_data as a
    where u.account_id = new.account_id
      and a.account_id = new.account_id
      and store_admins_search_view.id = new.account_id;

    return null;
    end;
$$ language plpgsql;


create trigger update_store_admins_view_update_at_users
    after update on users
    for each row
    execute procedure update_store_admins_view_update_fn();
