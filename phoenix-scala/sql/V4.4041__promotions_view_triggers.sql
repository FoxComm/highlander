create or replace function update_promotions_disc_links_view_update_fn() returns trigger as $$
declare prom_ids integer[];
begin
  case tg_table_name
    when 'promotion_discount_links' then
      select array_agg(p.form_id) into strict prom_ids
      from promotions as p
        where p.id = new.left_id;
    when 'discounts' then
      select array_agg(p.form_id) into strict prom_ids
      from promotions as p
        inner join promotion_discount_links as link on (p.id = link.left_id)
        where new.id = link.right_id;
  end case;

  update promotion_discount_links_view set
    promotion_id = q.promotion_id,
    discounts = q.discounts
    from (select
    p.form_id as promotion_id,
    -- discounts
    case when count(discount) = 0
      then
        '[]'::jsonb
    else
        json_agg(discount.form_id)::jsonb
    end as discounts

    from promotions as p
      inner join object_contexts as context on (p.context_id = context.id)
      inner join promotion_discount_links as link on (link.left_id = p.id)
      inner join discounts as discount on (discount.id = link.right_id)
      where p.form_id = any(prom_ids)
      group by p.form_id, context.id) as q
    where promotion_discount_links_view.promotion_id = q.promotion_id;

    return null;
end;
$$ language plpgsql;
