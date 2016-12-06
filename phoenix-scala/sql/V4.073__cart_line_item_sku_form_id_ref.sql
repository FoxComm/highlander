--alter table skus add constraint skus_form_id UNIQUE (form_id);

alter table cart_line_items
    drop constraint cart_line_item_skus_sku_id_fkey,
    add  constraint cart_line_items_sku_id_fkey FOREIGN KEY (sku_id) REFERENCES skus(id);

update cart_line_items
    set sku_id = q.form_id from (
        select id, form_id from skus
    ) as q where cart_line_items.sku_id = q.id;
