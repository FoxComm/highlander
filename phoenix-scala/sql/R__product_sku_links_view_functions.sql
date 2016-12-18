create or replace function get_variants_for_product(int) returns jsonb as $$
declare
    variants jsonb;
begin
    select
      case when count(sku) = 0
        then
            '[]'::jsonb
        else
        json_agg(sku.code)::jsonb
      end into variants
    from product__variant_links as link
    inner join product_variants as pvariant on (pvariant.id = link.right_id)
    where link.left_id = $1;
  if (variants = '[]'::jsonb) then
    select
      case when count(sku) = 0
        then
            '[]'::jsonb
        else
        json_agg(distinct sku.code)::jsonb
      end into variants
      from product__option_links as polink
        inner join product_option__value_links as vvlink on (polink.right_id = vvlink.left_id)
        inner join product_value__variant_links as vsku_link on (vsku_link.left_id = vvlink.right_id)
        inner join product_variants as variants on (vsku_link.right_id = variants.id)
      where polink.left_id = $1;
  end if;

  return variants;

end;
$$ language plpgsql;


create or replace function insert_product_sku_links_view_from_products_fn() returns trigger as $$
begin

  insert into product_sku_links_view select
    new.id as product_id,
    get_variants_for_product(new.id) as skus;

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
      inner join product_variant_links as link on link.left_id = p.id
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
    variants = subquery.variants
    from (select
            p.id,
            get_variants_for_product(p.id) variants
          from products as p
         where p.id = any(product_ids)
         group by p.id) as subquery
    where subquery.id = product__variant_links_view.product_id;
    return null;
end;
$$ language plpgsql;
