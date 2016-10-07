alter table shared_searches add column is_system boolean default false;

create or replace function share_system_searches_with_new_admins_fn() returns trigger as $$
begin
    insert into shared_search_associations (shared_search_id, store_admin_id)
    select id, new.id from shared_searches where is_system = true;

    return null;
end;
$$ language plpgsql;

create trigger share_system_searches_with_new_admins_trigger
    after insert on store_admins
    for each row
    execute procedure share_system_searches_with_new_admins_fn();