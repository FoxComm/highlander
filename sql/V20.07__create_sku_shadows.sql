create table sku_shadows(
    id serial primary key,
    context_id integer references object_contexts(id) on update restrict on delete restrict,
    sku_id integer not null references skus(id) on update restrict on delete restrict,
    attributes jsonb,
    active_from timestamp without time zone null,
    active_to timestamp without time zone null,
    created_at timestamp without time zone default (now() at time zone 'utc'),

    foreign key (sku_id) references skus(id) on update restrict on delete restrict,
    foreign key (context_id) references object_contexts(id) on update restrict on delete restrict
);

create unique index sku_shadows_idx on sku_shadows (id);
create index sku_shadows_sku_idx on sku_shadows (sku_id);
create index sku_shadows_sku_id_context_idx on sku_shadows (sku_id, context_id);
create index sku_shadows_sku_product_idx on sku_shadows (context_id);

create function create_order_line_item_skus_mapping() returns trigger as $$
begin
    insert into order_line_item_skus (sku_shadow_id, sku_id) values (new.id, new.sku_id);
    return new;
end;
$$ language plpgsql;

create trigger create_order_line_item_skus_mapping
    after insert
    on sku_shadows
    for each row
    execute procedure create_order_line_item_skus_mapping();

