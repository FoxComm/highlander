create table orders_search_view
(
    id bigint not null unique,
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


-- update on orders changes
create or replace function update_orders_view_from_orders_fn() returns trigger as $$
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
            ) as customer
            from customers as c
            left join customers_ranking as rank on (c.id = rank.id)
            where (new.customer_id = c.id)
          -- update only order stuff
on conflict do update set
    id = excluded.id,
    reference_number = excluded.reference_number,
    state = excluded.state,
    created_at = excluded.created_at,
    placed_at = excluded.placed_at,
    currency = excluded.currency;

      return null;
  end;
$$ language plpgsql;


create trigger update_orders_view_from_orders
    after update or insert on orders
    for each row
    execute procedure update_orders_view_from_orders_fn();

-- update on customers changes
create or replace function update_orders_view_from_customers_fn() returns trigger as $$
  begin
    return null;
  end;
$$ language plpgsql;