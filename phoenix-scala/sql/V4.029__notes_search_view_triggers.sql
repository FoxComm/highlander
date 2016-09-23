drop materialized view notes_search_view;

drop materialized view notes_admins_view;
drop materialized view notes_orders_view;
drop materialized view notes_customers_view;
drop materialized view notes_gift_cards_view;
drop materialized view notes_skus_view;
drop materialized view notes_products_view;
drop materialized view notes_promotions_view;
drop materialized view notes_coupons_view;
drop materialized view order_stats_view;


--- change types
alter table export_skus_raw drop column attributes;
alter table export_products_raw drop column attributes;
alter table export_promotions_raw drop column attributes;
alter table export_coupons_raw drop column attributes;
--

alter domain note_reference_type
        drop constraint note_reference_type_check;

alter domain note_reference_type
        add constraint note_reference_type_check
            check (value in ('cord', 'giftCard', 'customer', 'return', 'product', 'sku',
                                                        'promotion', 'coupon', 'storeAdmin'));




create table notes_search_view
(
  -- Note
    id integer unique,
    reference_id integer,
    reference_type note_reference_type,
    body note_body,
    priority generic_string,
    created_at json_timestamp not null,
    deleted_at json_timestamp,
    author jsonb not null,
    -- OneOf optional entity
    "cord" jsonb,
    customer jsonb,
    gift_card jsonb,
    sku_item jsonb,
    product jsonb,
    promotion jsonb,
    coupon jsonb,
    store_admin jsonb
);

create index notes_search_view_ref_idx on notes_search_view(reference_id, reference_type);

create or replace function update_notes_search_view_insert_fn() returns trigger as $$
declare new_note notes_search_view%rowtype;
begin

    select distinct on (new.id) into strict new_note
      new.id as id,
      new.reference_id as reference_id,
      new.reference_type as reference_type,
      new.body as body,
      new.priority as priority,
      to_char(n.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as created_at,
      to_char(n.deleted_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as deleted_at,
      to_json((admins.email, admins.name, admins.department)::export_store_admins) as author
    from notes as n
      inner join store_admins as admins on (n.store_admin_id = admins.id)
    where n.id = new.id;

    case new.reference_type
       when 'cord' then
        select
          case cords.is_cart
            when false then
                json_agg((
                        o.customer->>'id',
                        o.reference_number,
                        o.state,
                        o.placed_at,
                        o.sub_total,
                        o.shipping_total,
                        o.adjustments_total,
                        o.taxes_total,
                        o.grand_total,
                        o.line_item_count
                      )::export_orders)
              when true then
                json_agg((
                        carts.customer->>'id',
                        carts.reference_number,
                        null,
                        null,
                        carts.sub_total,
                        carts.shipping_total,
                        carts.adjustments_total,
                        carts.taxes_total,
                        carts.grand_total,
                        carts.line_item_count
                      )::export_orders)
            end into strict new_note.cord
              from cords
                left join orders_search_view as o on (o.reference_number = cords.reference_number and cords.is_cart = false)
                left join carts_search_view as carts on (carts.reference_number = cords.reference_number and cords.is_cart = true)
                where cords.id = new.reference_id
                group by cords.is_cart;
      when 'customer' then
          select
            to_json((
                  c.id,
                  c.name,
                  c.email,
                  c.is_blacklisted,
                  to_char(c.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
              )::export_customers) into strict new_note.customer
            from customers as c
            where c.id = new.reference_id;
      when 'giftCard' then
          select
            to_json((gc.code, gc.origin_type, gc.currency, to_char(gc.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'))::export_gift_cards)
              into strict new_note.gift_card
            from gift_cards as gc
            where gc.id = new.reference_id;
      when 'sku' then
        select distinct on(s.form_id)
            to_json((
                f.id,
                s.code,
                to_char(s.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
            )::export_skus_raw)
              into strict new_note.sku_item
            from skus as s
            inner join object_forms as f on (f.id = s.form_id and f.kind = 'sku')
            where s.form_id = new.reference_id;
      when 'product' then
          select distinct on(p.form_id)
                to_json((
                f.id,
                to_char(p.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
            )::export_products_raw)
            into strict new_note.product
            from products as p
            inner join object_forms as f on (f.id = p.form_id and f.kind = 'product')
            where p.form_id = new.reference_id;

      when 'promotion' then
          select distinct on(p.form_id)
                to_json((
                    f.id,
                    p.apply_type,
                    to_char(p.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
                )::export_promotions_raw)
            into strict new_note.promotion
            from promotions as p
            inner join object_forms as f on (f.id = p.form_id and f.kind = 'promotion')
            where p.form_id = new.reference_id;
      when 'coupon' then
          select distinct on(c.form_id)
                to_json((
                  f.id,
                  c.promotion_id,
                  to_char(c.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
              )::export_coupons_raw)
            into strict new_note.coupon
            from coupons as c
            inner join object_forms as f on (f.id = c.form_id and f.kind = 'coupon')
            where c.form_id = new.reference_id;
      when 'storeAdmin' then
          select
                  to_json((admins.email, admins.name, admins.department)::export_store_admins)
              into strict new_note.store_admin
              from store_admins as admins
              where admins.id = new.reference_id;
    end case;

    insert into notes_search_view select new_note.*;

    return null;
end;
$$ language plpgsql;


create or replace function update_notes_search_view_update_fn() returns trigger as $$
begin
  update notes_search_view set
      body = new.body,
      priority = new.priority,
      created_at = to_char(new.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
      deleted_at = to_char(new.deleted_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
    where notes_search_view.id = new.id;
  return null;
end;
$$ language plpgsql;

create trigger update_notes_search_view_insert
    after insert on notes
    for each row
    execute procedure update_notes_search_view_insert_fn();

create trigger update_notes_search_view_update
    after update on notes
    for each row
    execute procedure update_notes_search_view_update_fn();
