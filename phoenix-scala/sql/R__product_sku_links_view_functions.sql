create or replace function get_skus_for_product(int) returns jsonb as $$
declare skus jsonb;
begin
    select
      case when count(sku) = 0
        then
            '[]'::jsonb
        else
        json_agg(sku.form_id)::jsonb
      end into skus
    from product_sku_links as link
    inner join skus as sku on (sku.id = link.right_id)
    where link.left_id = $1;
  if (skus = '[]'::jsonb) then
    select
      case when count(sku) = 0
        then
            '[]'::jsonb
        else
        json_agg(distinct sku.form_id)::jsonb
      end into skus
      from product_variant_links as pvlink
        inner join variant_variant_value_links as vvlink on (pvlink.right_id = vvlink.left_id)
        inner join variant_value_sku_links as vsku_link on (vsku_link.left_id = vvlink.right_id)
        inner join skus as sku on (vsku_link.right_id = sku.id)
      where pvlink.left_id = $1;
  end if;

  return skus;

end;
$$ language plpgsql;


create or replace function insert_product_sku_links_view_from_products_fn() returns trigger as $$
begin

  insert into product_sku_links_view select
    new.id as product_id,
    get_skus_for_product(new.id) as skus;

    return null;
end;
$$ language plpgsql;


create or replace function update_product_sku_links_view_from_products_and_deps_fn() returns trigger as $$
declare product_ids int[];
begin
  case tg_table_name
    when 'products' then
      product_ids := array_agg(new.id);
    when 'product_sku_links' then
      select array_agg(p.id) into product_ids
      from products as p
      inner join product_sku_links as link on (link.left_id = p.id)
      where link.id = new.id;
    when 'skus' then
      select array_agg(p.id) into product_ids
      from products as p
      inner join product_sku_links as link on link.left_id = p.id
      inner join skus as sku on (sku.id = link.right_id)
      where sku.id = new.id;
    when 'variant_value_sku_links' then
      select array_agg(p.id) into product_ids
      from products as p
      inner join product_variant_links as pvlink on (pvlink.left_id = p.id)
      inner join variant_variant_value_links as vvlink on (pvlink.right_id = vvlink.left_id)
      where vvlink.right_id = (case TG_OP
                            when 'DELETE' then
                              old.left_id
                            else
                              new.left_id
                          end);
  end case;

  update product_sku_links_view set
    skus = subquery.skus
    from (select
            p.id,
            get_skus_for_product(p.id) skus
          from products as p
         where p.id = any(product_ids)
         group by p.id) as subquery
    where subquery.id = product_sku_links_view.product_id;
    return null;
end;
$$ language plpgsql;
