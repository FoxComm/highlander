create or replace function update_inventory_transactions_view_from_transactions_insert_fn() returns trigger as $$
declare
    skuCode sku_code;
    stockLocationName generic_string;
begin
    skuCode := (
        select si.sku
        from stock_items si
        where si.id = new.stock_item_id
    );
    stockLocationName := (
        select sl.name
        from stock_items as si
        left join stock_locations sl ON sl.id = si.stock_location_id
        where (new.stock_item_id = si.id)
    );

    insert into inventory_transactions_search_view select distinct on (new.id)
        new.id as id,
        skuCode as sku,
        stockLocationName as stock_location_name,
        new.type as type,
        new.status as status,
        new.amount_new - new.amount_change as amount_previous,
        new.amount_new as amount_new,
        new.amount_change as amount_change,
        new.afs_new as afs_new,
        to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at;
    return null;
end;
$$ language plpgsql;

create trigger update_inventory_transactions_view_from_transactions_insert
    after insert on stock_item_transactions
    for each row
    execute procedure update_inventory_transactions_view_from_transactions_insert_fn();
