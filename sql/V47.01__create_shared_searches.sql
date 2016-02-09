create table shared_searches (
    id serial primary key,
    title generic_string not null,
    code generic_string not null unique,
    scope shared_search_scope not null,
    query jsonb not null,
    store_admin_id integer not null references store_admins(id) on update restrict on delete restrict,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null
);

create index shared_searches_store_admin_id on shared_searches (store_admin_id);

create function generate_shared_search_code(len integer) returns text AS $$
declare
    new_code text;
    done bool;
begin
    done := false;
    while not done loop
        new_code := lower(substr(md5(random()::text), 0, len + 1));
        done := not exists(select 1 from shared_searches WHERE code = new_code);
    end loop;
    return new_code;
end;
$$ language plpgsql;

create function set_shared_searches_code() returns trigger as $$
begin
    new.code = generate_shared_search_code(16);
    return new;
end;
$$ language plpgsql;

create trigger set_shared_searches_code_trigger
    before insert
    on shared_searches
    for each row
    execute procedure set_shared_searches_code();