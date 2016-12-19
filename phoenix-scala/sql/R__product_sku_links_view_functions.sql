create or replace function get_skus_for_product(int) returns jsonb as $$
-- return list of sku codes from variants from product
declare
    skus jsonb;
begin
    select
      case when count(pvariant) = 0
        then
            '[]'::jsonb
        else
        json_agg(pvariant.code)::jsonb
      end into skus
    from product__variant_links as link
    inner join product_variants as pvariant on (pvariant.id = link.right_id)
    where link.left_id = $1;

  if (skus = '[]'::jsonb) then
    select
      case when count(pvariants) = 0
        then
            '[]'::jsonb
        else
        json_agg(distinct pvariants.code)::jsonb
      end into skus
      from product__option_links as polink
        inner join product_option__value_links as vvlink on (polink.right_id = vvlink.left_id)
        inner join product_value__variant_links as vsku_link on (vsku_link.left_id = vvlink.right_id)
        inner join product_variants as pvariants on (vsku_link.right_id = pvariants.id)
      where polink.left_id = $1;
  end if;

  return skus;

end;
$$ language plpgsql;


create or replace function insert_product_sku_links_view_from_products_fn() returns trigger as $$
begin

  insert into product__variant_links_view select
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
    when 'product__variant_links' then
      select array_agg(p.id) into product_ids
      from products as p
      inner join product__variant_links as link on (link.left_id = p.id)
      where link.id = new.id;
    when 'product_variants' then
      select array_agg(p.id) into product_ids
      from products as p
      inner join product__variant_links as link on link.left_id = p.id
      inner join product_variants as sku on (sku.id = link.right_id)
      where sku.id = new.id;
    when 'product_value__variant_links' then
      select array_agg(p.id) into product_ids
      from products as p
      inner join product_option_links as polink on (polink.left_id = p.id)
      inner join product_value__variant_links as vvlink on (polink.right_id = vvlink.left_id)
      where vvlink.right_id = (case TG_OP
                            when 'DELETE' then
                              old.left_id
                            else
                              new.left_id
                          end);
  end case;

  update product__variant_links_view set
    skus = subquery.skus
    from (select
            p.id,
            get_skus_for_product(p.id) skus
          from products as p
         where p.id = any(product_ids)
         group by p.id) as subquery
    where subquery.id = product__variant_links_view.product_id;
    return null;
end;
$$ language plpgsql;
