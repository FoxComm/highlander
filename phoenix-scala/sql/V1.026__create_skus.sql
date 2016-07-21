create table skus(
    id serial primary key,
    code sku_code not null,
    context_id integer not null references object_contexts(id) on update restrict on delete restrict,
    shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
    form_id integer not null references object_forms(id) on update restrict on delete restrict,
    commit_id integer references object_commits(id) on update restrict on delete restrict,
    updated_at generic_timestamp,
    created_at generic_timestamp,
    archived_at generic_timestamp
);

create unique index skus_code_context_id on skus (lower(code), context_id);

create function create_order_line_item_skus_mapping() returns trigger as $$
begin
    insert into order_line_item_skus (sku_shadow_id, sku_id) values (new.shadow_id, new.id);
    return new;
end;
$$ language plpgsql;

create trigger create_order_line_item_skus_mapping
    after insert on skus for each row
    execute procedure create_order_line_item_skus_mapping();

