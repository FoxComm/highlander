-- optional entities updating triggers

-- authors
create or replace function update_notes_search_view_on_admins_fn() returns trigger as $$
begin
  update notes_search_view set author = to_json((new.email, new.name)::export_store_admins)
  where notes_search_view.id IN (select notes.id from notes where notes.store_admin_id = new.account_id);

  return null;
end;
$$ language plpgsql;

create trigger update_notes_search_view_on_users
    after update on users
    for each row
    when (old.name is distinct from new.name or
          old.email is distinct from new.email)
    execute procedure update_notes_search_view_on_admins_fn();

-- orders
create or replace function update_notes_search_view_on_orders_fn() returns trigger as $$
begin

  update notes_search_view set "cord" = q.order from (select
    json_agg((
             new.customer->>'id',
             new.reference_number,
             new.state,
             new.placed_at,
             new.sub_total,
             new.shipping_total,
             new.adjustments_total,
             new.taxes_total,
             new.grand_total,
             new.line_item_count
           )::export_orders) as "order") as q
      where notes_search_view.reference_id = new.id and notes_search_view.reference_type = 'cord';

  return null;
end;
$$ language plpgsql;

create or replace function update_notes_search_view_on_carts_fn() returns trigger as $$
 begin

 update notes_search_view set "cord" = q.cord from (select
       json_agg((
           new.customer->>'id',
           new.reference_number,
           null,
           null,
           new.sub_total,
           new.shipping_total,
           new.adjustments_total,
           new.taxes_total,
           new.grand_total,
           new.line_item_count
         )::export_orders) as cord) as q
       where notes_search_view.reference_id = new.id and notes_search_view.reference_type = 'cord';

 return null;
end;
$$ language plpgsql;

create trigger update_notes_search_view_on_orders
    after update on orders_search_view
    for each row
    execute procedure update_notes_search_view_on_orders_fn();

create trigger update_notes_search_view_on_carts
    after update on carts_search_view
    for each row
    execute procedure update_notes_search_view_on_carts_fn();


-- customer

create or replace function update_notes_search_view_on_customer_fn() returns trigger as $$
begin

  update notes_search_view set customer = q.customer from (select to_json((
              c.id,
              new.account_id,
              new.name,
              new.email,
              new.is_blacklisted,
              to_json_timestamp(new.created_at)
          )::export_customers) as customer
    from customer_data as c where c.user_id = new.id) as q
    where notes_search_view.reference_id = new.account_id and notes_search_view.reference_type = 'customer';
  return null;
end;
$$ language plpgsql;

create trigger update_notes_search_view_on_customer
    after update on users
    for each row
    execute procedure update_notes_search_view_on_customer_fn();


-- giftCard
create or replace function update_notes_search_view_on_giftCard_fn() returns trigger as $$
begin

  update notes_search_view set
    gift_card = to_json((new.code, new.origin_type,
                         new.currency,
                         to_json_timestamp(new.created_at))::export_gift_cards)
    where notes_search_view.reference_id = new.id and notes_search_view.reference_type = 'giftCard';

  return null;
end;
$$ language plpgsql;

create trigger update_notes_search_view_on_giftCard
    after update on gift_cards
    for each row
    execute procedure update_notes_search_view_on_giftCard_fn();

-- sku
create or replace function update_notes_search_view_on_sku_fn() returns trigger as $$
begin

  update notes_search_view set sku_item = q.sku from
          (select
            to_json((
                f.id,
                s.code,
                to_json_timestamp(s.created_at)
            )::export_skus_raw)
              as sku
            from skus as s
            inner join object_forms as f on (f.id = s.form_id and f.kind = 'sku')
            where s.form_id = new.form_id) as q
        where notes_search_view.reference_id = new.form_id and notes_search_view.reference_type = 'sku';

  return null;
end;
$$ language plpgsql;

create trigger update_notes_search_view_on_sku
    after update on skus
    for each row
    execute procedure update_notes_search_view_on_sku_fn();

-- product
create or replace function update_notes_search_view_on_product_fn() returns trigger as $$
begin
  update notes_search_view set product = q.product from
      (select
            to_json((
            f.id,
            to_json_timestamp(p.created_at)
        )::export_products_raw)
        as product
        from products as p
        inner join object_forms as f on (f.id = p.form_id and f.kind = 'product')
        where p.form_id = new.form_id) as q
    where notes_search_view.reference_id = new.form_id and notes_search_view.reference_type = 'product';

  return null;
end;
$$ language plpgsql;

create trigger update_notes_search_view_on_product
    after update on products
    for each row
    execute procedure update_notes_search_view_on_product_fn();

-- promotion

create or replace function update_notes_search_view_on_promotion_fn() returns trigger as $$
begin
  update notes_search_view set promotion = q.promotion from
          (select
                to_json((
                    f.id,
                    p.apply_type,
                    to_json_timestamp(p.created_at)
                )::export_promotions_raw)
            as promotion
            from promotions as p
            inner join object_forms as f on (f.id = p.form_id and f.kind = 'promotion')
            where p.form_id = new.form_id) as q
        where notes_search_view.reference_id = new.form_id and notes_search_view.reference_type = 'promotion';

  return null;
end;
$$ language plpgsql;

create trigger update_notes_search_view_on_promotion
    after update on promotions
    for each row
    execute procedure update_notes_search_view_on_promotion_fn();

-- coupon

create or replace function update_notes_search_view_on_coupon_fn() returns trigger as $$
begin
    update notes_search_view set coupon = q.coupon from
          (select
                to_json((
                  f.id,
                  c.promotion_id,
                  to_json_timestamp(c.created_at)
              )::export_coupons_raw)
            as coupon
            from coupons as c
            inner join object_forms as f on (f.id = c.form_id and f.kind = 'coupon')
            where c.form_id = new.form_id) as q
        where notes_search_view.reference_id = new.form_id and notes_search_view.reference_type = 'coupon';

  return null;
end;
$$ language plpgsql;

create trigger update_notes_search_view_on_coupon
    after update on coupons
    for each row
    execute procedure update_notes_search_view_on_coupon_fn();
