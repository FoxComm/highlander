create table return_line_item_skus (
    id integer primary key,
    return_id integer not null references returns(id) on update restrict on delete restrict,
    sku_id integer not null references skus(id) on update restrict on delete restrict,
    sku_shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
    created_at generic_timestamp,
    foreign key (id) references return_line_item_origins(id) on update restrict on delete restrict
);

create index return_line_item_skus_return_idx on return_line_item_skus (return_id);

create trigger set_return_line_item_sku_id
    before insert
    on return_line_item_skus
    for each row
    execute procedure set_return_line_item_origin_id();

