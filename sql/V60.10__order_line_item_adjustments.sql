create table order_line_item_adjustments (
    id serial primary key,
    order_id integer not null references orders(id) on update restrict on delete restrict,
    promotion_shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
    adjustment_type generic_string not null,
    subtract integer not null,
    line_item_id integer references order_line_items(id) on update restrict on delete restrict,
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create index order_line_item_adjustments_order_idx on order_line_item_adjustments (order_id);
create index order_line_item_adjustments_line_item_idx on order_line_item_adjustments (line_item_id);

