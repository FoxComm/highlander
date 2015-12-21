create materialized view orders_search_view as
select distinct on (o.id)
    -- Order
    o.id as id,
    o.reference_number as reference_number,
    o.status as status,
    to_char(o.created_at, 'YYYY-MM-DD HH24:MI:SS') as created_at,
    to_char(o.placed_at, 'YYYY-MM-DD HH24:MI:SS') as placed_at,
    o.currency as currency,
    -- Totals
    o.sub_total as sub_total,
    o.shipping_total as shipping_total,
    o.adjustments_total as adjustments_total,
    o.taxes_total as taxes_total,
    o.grand_total as grand_total,
    -- Customer
    json_build_object(
        'name', c.name,
        'email', c.email,
        'is_blacklisted', c.is_blacklisted,
        'joined_at', to_char(c.created_at, 'YYYY-MM-DD HH24:MI:SS'),
        'rank', rank.rank,
        'revenue', coalesce(rank.revenue, 0)
    ) as customer,
    -- Line items
    li.count as line_item_count,
    li.items as line_items,
    -- Payments
    p.payments as payments,
    ccp.count as credit_card_count,
    ccp.total as credit_card_total,
    gcp.count as gift_card_count,
    gcp.total as gift_card_total,
    scp.count as store_credit_count,
    scp.total as store_credit_total,
    -- Shipments
    s.count as shipment_count,
    s.shipments as shipments,
    -- Addresses
    osa.count as shipping_addresses_count,
    osa.addresses as shipping_addresses,
    oba.count as billing_addresses_count,
    oba.addresses as billing_addresses,
    -- Assignments
    ass.count as assignment_count,
    ass.assignees as assignees,
    -- RMAs
    rma.count as rma_count,
    rma.rmas as rmas
from orders as o
inner join customers as c on (o.customer_id = c.id)
left join customers_ranking as rank on (c.id = rank.id)
inner join order_line_items_view as li on (o.id = li.order_id)
inner join order_payments_view as p on (o.id = p.order_id)
inner join order_credit_card_payments_view as ccp on (o.id = ccp.order_id)
inner join order_gift_card_payments_view as gcp on (o.id = gcp.order_id)
inner join order_store_credit_payments_view as scp on (o.id = scp.order_id)
inner join order_shipments_view as s on (o.id = s.order_id)
inner join order_shipping_addresses_view as osa on (o.id = osa.order_id)
inner join order_billing_addresses_view as oba on (o.id = oba.order_id)
inner join order_assignments_view as ass on (o.id = ass.order_id)
inner join order_rmas_view as rma on (o.id = rma.order_id)
order by o.id;

create unique index orders_search_view_idx on orders_search_view (id);
