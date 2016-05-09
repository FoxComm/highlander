create table orders_search_view
(
    id bigint not null,
    reference_number reference_number not null unique,
    state generic_string not null,
    created_at text,
    placed_at text,
    currency currency,
    sub_total integer not null default 0,
    shipping_total integer not null default 0,
    adjustments_total integer not null default 0,
    taxes_total integer not null default 0,
    grand_total integer not null default 0,
    customer jsonb not null,
    line_item_count bigint,
    line_items jsonb,
    payments jsonb,
    credit_card_count bigint,
    credit_card_total bigint,
    gift_card_count bigint,
    gift_card_total bigint,
    store_credit_count bigint,
    store_credit_total bigint,
    shipment_count bigint,
    shipments jsonb,
    shipping_addresses_count bigint,
    shipping_addresses jsonb,
    billing_addresses_count bigint,
    billing_addresses jsonb,
    assignment_count bigint,
    assignees jsonb,
    rma_count bigint,
    rmas jsonb
);

create unique index orders_search_view_idx on orders_search_view (id);

create or replace function update_orders_view_from_orders_fn() returns trigger as $emp_stamp$
    begin
        insert into orders_search_view select distinct on (new.id)
            -- order
            new.id as id,
            new.reference_number as reference_number,
            new.state as state,
            to_char(new.created_at, 'yyyy-mm-dd"t"hh24:mi:ss.ms"z"') as created_at,
            to_char(new.placed_at, 'yyyy-mm-dd"t"hh24:mi:ss.ms"z"') as placed_at,
            new.currency as currency,
            -- totals
            new.sub_total as sub_total,
            new.shipping_total as shipping_total,
            new.adjustments_total as adjustments_total,
            new.taxes_total as taxes_total,
            new.grand_total as grand_total,
            -- customer
            json_build_object(
                'id', c.id,
                'name', c.name,
                'email', c.email,
                'is_blacklisted', c.is_blacklisted,
                'joined_at', to_char(c.created_at, 'yyyy-mm-dd"t"hh24:mi:ss.ms"z"'),
                'rank', rank.rank,
                'revenue', coalesce(rank.revenue, 0)
            ) as customer,
            -- line items
            li.count as line_item_count,
            li.items as line_items,
            -- payments
            p.payments as payments,
            ccp.count as credit_card_count,
            ccp.total as credit_card_total,
            gcp.count as gift_card_count,
            gcp.total as gift_card_total,
            scp.count as store_credit_count,
            scp.total as store_credit_total,
            -- shipments
            s.count as shipment_count,
            s.shipments as shipments,
            -- addresses
            osa.count as shipping_addresses_count,
            osa.addresses as shipping_addresses,
            oba.count as billing_addresses_count,
            oba.addresses as billing_addresses,
            -- assignments
            a.count as assignment_count,
            a.assignees as assignees,
            -- rmas
            rma.count as rma_count,
            rma.rmas as rmas
            from customers as c
            left join customers_ranking as rank on (c.id = rank.id)
            left join order_line_items_view as li on (new.id = li.order_id)
            left join order_payments_view as p on (new.id = p.order_id)
            left join order_credit_card_payments_view as ccp on (new.id = ccp.order_id)
            left join order_gift_card_payments_view as gcp on (new.id = gcp.order_id)
            left join order_store_credit_payments_view as scp on (new.id = scp.order_id)
            left join order_shipments_view as s on (new.id = s.order_id)
            left join order_shipping_addresses_view as osa on (new.id = osa.order_id)
            left join order_billing_addresses_view as oba on (new.id = oba.order_id)
            left join order_assignments_view as a on (new.id = a.order_id)
            left join order_rmas_view as rma on (new.id = rma.order_id)
            where (new.customer_id = c.id)
          -- update only order stuff
on conflict (id) do update set
    id = excluded.id,
    reference_number = excluded.reference_number,
    state = excluded.state,
    created_at = excluded.created_at,
    placed_at = excluded.placed_at,
    currency = excluded.currency;

      return null;
  end;
$emp_stamp$ language plpgsql;


create trigger update_orders_view_from_orders
    after update or insert on orders
    for each row
    execute procedure update_orders_view_from_orders_fn();

-- drop trigger if exists update_orders_view_from_orders on orders;