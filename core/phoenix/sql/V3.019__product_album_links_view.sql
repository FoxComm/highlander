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
            p.id as product_id,
            case when count(asv) = 0
              then
                  '[]'::jsonb
              else
                 json_agg((asv.name, asv.images)::export_albums)::jsonb
            end as albums
          from products as p
            left join product_album_links as link on link.left_id = p.id
            left join albums as album on album.id = link.right_id
            left join album_search_view as asv on album.id = asv.album_id
         where p.id = any(product_ids)
         group by p.id) as subquery
    where subquery.product_id = product_album_links_view.product_id;
    return null;
end;
$$ language plpgsql;

drop trigger update_product_album_links_view_from_album_image_links on album_image_links restrict;

create trigger update_product_album_links_view_from_album_search_view
after update or insert on album_search_view
for each row
execute procedure update_product_album_links_view_from_products_and_deps_fn();

drop trigger update_product_album_links_view_from_product_album_links on product_album_links;
create trigger update_product_album_links_view_from_product_album_links
after update or insert or delete on product_album_links
for each row
execute procedure update_product_album_links_view_from_products_and_deps_fn();
