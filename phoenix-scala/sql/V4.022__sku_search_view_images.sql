create or replace function get_images_json_for_album(int) returns jsonb as $$
begin
    return case when count(imgs) = 0
           then '[]'
           else json_agg(imgs)
           end from (select
            (form.attributes ->> (shadow.attributes -> 'src' ->> 'ref')) as src,
            (form.attributes ->> (shadow.attributes -> 'alt' ->> 'ref')) as alt,
            (form.attributes ->> (shadow.attributes -> 'title' ->> 'ref')) as title
    from images as image
    inner join album_image_links as lnk on (lnk.right_id = image.id)
    inner join object_forms as form on (form.id = image.form_id)
    inner join object_shadows as shadow on (shadow.id = image.shadow_id)
    where lnk.left_id = $1
    order by lnk.position) as imgs;
end;
$$ language plpgsql;

create or replace function get_albums_json_for_product(int) returns jsonb as $$
begin
  return case when count(albums) = 0
         then '[]'
         else json_agg(albums)
         end from (select asv.name as "name", asv.images as "images"
    from album_search_view as asv
      inner join product_album_links as link on asv.album_id = link.right_id
  where link.left_id = $1
  order by link.position) as albums;
end;
$$ language plpgsql;

create or replace function update_product_album_links_view_from_products_and_deps_fn() returns trigger as $$
declare product_ids int[];
begin
  case tg_table_name
    when 'products' then
      product_ids := array_agg(new.id);
    when 'product_album_links' then
      select array_agg(p.id) into product_ids
      from products as p
      where p.id = (case TG_OP
                    when 'DELETE' then old.left_id
                       else new.left_id
                       end);
    when 'albums' then
      select array_agg(p.id) into product_ids
      from products as p
      inner join product_album_links as link on link.left_id = p.id
      inner join albums as album on (album.id = link.right_id)
      where album.id = new.id;
    when 'album_search_view' then
      select array_agg(p.id) into product_ids
      from products as p
      inner join product_album_links as link on link.left_id = p.id
      inner join albums on (albums.id = link.right_id)
      where albums.id = new.album_id;
    when 'images' then
      select array_agg(p.id) into product_ids
      from products as p
      inner join product_album_links as link on link.left_id = p.id
      inner join albums as album on (album.id = link.right_id)
      inner join album_image_links as img_link on (img_link.left_id = album.id)
      inner join images as image on (image.id = img_link.right_id)
      where image.id = new.id;
  end case;

  update product_album_links_view set
    albums = subquery.albums
  from (select
          p.id                              as product_id,
          get_albums_json_for_product(p.id) as albums
        from products as p
        where p.id = any(product_ids)) as subquery
    where subquery.product_id = product_album_links_view.product_id;
    return null;
end;
$$ language plpgsql;

create or replace function update_skus_view_image_fn() returns trigger as $$
declare sku_ids int[];
begin

  case tg_table_name
    when 'product_sku_links' then
    select array_agg(product_sku_links.right_id) into sku_ids
    from product_sku_links
    where product_sku_links.id = new.id;
    when 'product_album_links_view' then
      select array_agg(sku.id) into sku_ids
      from skus as sku
        inner join product_sku_links on sku.id = product_sku_links.right_id
      where product_sku_links.left_id = new.product_id;
  end case;

  update sku_search_view
  set image = subquery.image
  from (select sku.id as id,
               sku.code as code,
               (product_album_links_view.albums #>> '{0, images, 0, src}') as image
        from skus as sku
          inner join product_sku_links on sku.id = product_sku_links.right_id
          inner join product_album_links_view on product_sku_links.left_id = product_album_links_view.product_id
        where sku.id = any(sku_ids)) as subquery
  where subquery.id = sku_search_view.id;

    return null;
end;
$$ language plpgsql;


create trigger update_skus_view_image
after insert or update on product_album_links_view
for each row
execute procedure update_skus_view_image_fn();

create trigger update_skus_view_image
after insert or update on product_sku_links
for each row
execute procedure update_skus_view_image_fn();

