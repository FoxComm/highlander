create table sku_shadows(
    id serial primary key,
    product_context_id integer,
    sku_id integer,
    attributes jsonb,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    foreign key (product_context_id) references product_contexts(id) on update restrict on delete restrict
);

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

