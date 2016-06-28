create table order_line_item_adjustments (
    id serial primary key,
    order_ref text not null references orders(reference_number) on update restrict on delete restrict,
    promotion_shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
    adjustment_type generic_string not null,
    substract integer not null,
    line_item_ref_num generic_string null,
    created_at generic_timestamp
);

create index order_line_item_adjustments_order_idx on order_line_item_adjustments (order_ref);
