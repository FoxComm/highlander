create or replace function update_carts_view_from_carts_insert_fn() returns trigger as $$
    begin
        insert into carts_search_view select distinct on (new.id)
            -- order
            new.id as id,
            new.reference_number as reference_number,
            to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
            to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as updated_at,
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
                'joined_at', to_char(c.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
                'rank', rank.rank,
                'revenue', coalesce(rank.revenue, 0)
            )::jsonb as customer
            from customers as c
            left join customers_ranking as rank on (c.id = rank.id)
            where (new.customer_id = c.id);
        return null;
    end;
$$ language plpgsql;

create or replace function update_carts_view_from_carts_update_fn() returns trigger as $$
begin
    update carts_search_view set
        reference_number = new.reference_number,
        created_at = to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        updated_at = to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
        currency = new.currency,
        sub_total = new.sub_total,
        shipping_total = new.shipping_total,
        adjustments_total = new.adjustments_total,
        taxes_total = new.taxes_total,
        grand_total = new.grand_total
    where id = new.id;

    return null;
end;
$$ language plpgsql;

create or replace function update_carts_view_from_carts_delete_fn() returns trigger as $$
begin
    update carts_search_view set
        deleted_at = to_char(now(), 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
    where id = old.id;
    return null;
end;
$$ language plpgsql;

create trigger update_carts_view_from_carts_insert
    after insert on carts
    for each row
    execute procedure update_carts_view_from_carts_insert_fn();

create trigger update_carts_view_from_carts_update
    after update on carts
    for each row
    execute procedure update_carts_view_from_carts_update_fn();

create trigger update_carts_view_from_carts_delete
    after delete on carts
    for each row
    execute procedure update_carts_view_from_carts_delete_fn();
