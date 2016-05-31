create table product_sku_links_view
(
    product_id integer unique,
    skus jsonb
);

create or replace function insert_product_sku_links_view_from_products_fn() returns trigger as $$
begin

  insert into product_sku_links_view select
    NEW.id as product_id,
    case when count(sku) = 0
      then
          '[]'
      else
      json_agg(sku.code)
    end as skus
    from object_links as link
    left join skus as sku on sku.shadow_id = link.right_id
    where link.left_id = NEW.shadow_id;

    return null;
end;
$$ language plpgsql;

create or replace function update_product_sku_links_view_from_products_and_deps_fn() returns trigger as $$
declare product_ids int[];
begin
  case TG_TABLE_NAME
    when 'products' then
      product_ids := array_agg(NEW.id);
    when 'object_links' then
      select array_agg(p.id) into strict product_ids
      from products as p
      inner join object_links as link on (link.left_id = p.shadow_id)
      where link.id = NEW.id;
    when 'skus' then
      select array_agg(p.id) into strict product_ids
      from products as p
      inner join object_links as link on link.left_id = p.shadow_id
      inner join skus as sku on (sku.shadow_id = link.right_id)
      where sku.id = NEW.id;
  end case;

  update product_sku_links_view set
    skus = subquery.skus
    from (select
            p.id,
            case when count(sku) = 0
              then
                  '[]'
              else
              json_agg(sku.code)
            end as skus
          from products as p
            left join object_links as link on link.left_id = p.shadow_id
            left join skus as sku on sku.shadow_id = link.right_id
         where p.id = ANY(product_ids)
         group by p.id) as subquery
    where subquery.id = product_sku_links_view.product_id;
    return null;
end;
$$ language plpgsql;


create trigger insert_product_sku_links_view_from_products
    after insert on products
    for each row
    execute procedure insert_product_sku_links_view_from_products_fn();

create trigger update_product_sku_links_view_from_products
  after update on products
  for each row
  WHEN (OLD.shadow_id is distinct from NEW.shadow_id)
  execute procedure update_product_sku_links_view_from_products_and_deps_fn();

create trigger update_product_sku_links_view_from_object_links
  after update or insert on object_links
  for each row
  execute procedure update_product_sku_links_view_from_products_and_deps_fn();

create trigger update_product_sku_links_view_from_skus
  after update or insert on skus
  for each row
  execute procedure update_product_sku_links_view_from_products_and_deps_fn();
