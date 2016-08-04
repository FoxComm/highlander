create or replace function update_inventory_view_from_summary_insert_fn() returns trigger as $$
declare
  sku sku_code;
  type generic_string;
  stock_item jsonb;
  stock_location jsonb;
    begin
        sku := (select si.sku from stock_items si where si.id = new.stock_item_id);
        type := (select st.type from stock_item_types st where st.id = new.type_id);

        stock_item := (select json_build_object(
            'id', si.id,
            'sku', si.sku,
            'defaultUnitCost', si.default_unit_cost
        )::jsonb from stock_items as si
        where (new.stock_item_id = si.id));

        stock_location := (select json_build_object(
            'id', sl.id,
            'name', sl.name,
            'type', sl.type
        )::jsonb as stock_location
        from stock_items as si
        left join stock_locations sl ON sl.id = si.stock_location_id
        where (new.stock_item_id = sl.id));

        insert into inventory_search_view select distinct on (new.id)
            -- summary
            new.id as id,
            --sku
            sku,
            -- stock_item
            stock_item,
            -- stock_locatoin
            stock_location,

            -- type
            type,

            new.on_hand as on_hand,
            new.on_hold as on_hold,
            new.reserved as reserved,
            new.shipped as shipped,
            new.afs as afs,
            new.afs_cost as afs_cost,
            to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
            to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at,
            to_char(new.deleted_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as deleted_at;
      return null;
  end;
$$ language plpgsql;

create or replace function update_inventory_view_from_summary_update_fn() returns trigger as $$
begin
    update inventory_search_view set
        type = 'Sellable',
        on_hand = new.on_hand,
        on_hold = new.on_hold,
        reserved = new.reserved,
        shipped = new.shipped,
        afs = new.afs,
        afs_cost = new.afs_cost,
        updated_at = to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        deleted_at = to_char(new.deleted_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
    where id = new.id;
    return null;
    end;
$$ language plpgsql;

create trigger update_inventory_view_from_summary_insert
    after insert on stock_item_summaries
    for each row
    execute procedure update_inventory_view_from_summary_insert_fn();

create trigger update_inventory_view_from_summary_update
    after update on stock_item_summaries
    for each row
    execute procedure update_inventory_view_from_summary_update_fn();
