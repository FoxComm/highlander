create materialized view inventory_transactions_search_view as
select
    adj.id,
    to_char(adj.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
    wh.name as warehouse,
    adj.metadata->>'name' || ' ' || coalesce(adj.metadata->>'orderRef', '') as event,
    adj.new_quantity - change as previous_quantity,
    adj.new_quantity,
    adj.change,
    adj.new_afs,
    adj.sku_type,
    adj.state
from inventory_adjustments adj
inner join inventory_summaries sums on sums.id = adj.summary_id
inner join warehouses wh on wh.id = sums.warehouse_id
inner join sellable_inventory_summaries sel on sel.id = adj.summary_id
inner join preorder_inventory_summaries pre on pre.id = adj.summary_id
inner join backorder_inventory_summaries bac on bac.id = adj.summary_id
inner join nonsellable_inventory_summaries non on non.id = adj.summary_id
order by created_at desc
;

create unique index inventory_transactions_search_view_idx on inventory_transactions_search_view (id);
