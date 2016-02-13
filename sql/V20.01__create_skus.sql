create table skus (
    id serial primary key,
    product_id integer,
    sku generic_string,
    type generic_string,
    attributes jsonb,
    foreign key (product_context_id) references product_contexts(id) on update restrict on delete restrict
);

create function create_order_line_item_skus_mapping() returns trigger as $$
begin
    insert into order_line_item_skus (sku_id) values (new.id);
    return new;
end;
$$ language plpgsql;

create trigger create_order_line_item_skus_mapping
    after insert
    on skus
    for each row
    execute procedure create_order_line_item_skus_mapping();

