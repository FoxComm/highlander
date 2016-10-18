create or replace function get_scope_path(scope_id integer) returns generic_string as $$
declare
    parent_path exts.ltree;
    scope_path generic_string;
begin
    select scopes.parent_path from scopes where id = scope_id into parent_path;
    if nlevel(parent_path) > 0 then
        scope_path:= ltree2text(parent_path || scope_id::text);
    else
        scope_path:= scope_id::generic_string;
    end if;
    return scope_path;
end;
$$ LANGUAGE plpgsql;

create or replace function update_scope_parent_path_from_scope_insert_fn() returns trigger as $$
begin
    if new.parent_id is not null then
        update scopes set
            parent_path = text2ltree(get_scope_path(new.parent_id)::text)
            where id = new.id;
    end if;
    return null;
end;
$$ LANGUAGE plpgsql;

-- update parent_path when parent_id changes
create or replace function update_scope_parent_path_from_scope_update_fn() returns trigger as $$
begin
    if new.parent_id is not null then
        update scopes set
            parent_path = text2ltree(get_scope_path(new.parent_id)::text)
            where id = new.id;
    end if;
    return null;
end;
$$ LANGUAGE plpgsql;

-- update parent_path of children when parent_path changes
create or replace function update_scope_parent_path_from_scope_parent_update_fn() returns trigger as $$
begin
    update scopes set
        parent_path = text2ltree(get_scope_path(new.id)::text)
        where parent_id = new.id;
    return null;
end;
$$ LANGUAGE plpgsql;

create trigger update_scope_parent_path_from_scope_insert
    after insert on scopes
    for each row
    execute procedure update_scope_parent_path_from_scope_insert_fn();

create trigger update_scope_parent_path_from_scope_update
    after update on scopes
    for each row
    when (old.parent_id is distinct from new.parent_id)
    execute procedure update_scope_parent_path_from_scope_update_fn();

create trigger update_scope_parent_path_from_scope_parent_update
    after update on scopes
    for each row
    when (old.parent_path is distinct from new.parent_path)
    execute procedure update_scope_parent_path_from_scope_parent_update_fn();
