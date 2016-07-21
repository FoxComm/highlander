create table product_album_links_view
(
    product_id integer unique,
    albums jsonb
);

create or replace function insert_product_album_links_view_from_products_fn() returns trigger as $$
begin

  insert into product_album_links_view select
    new.id as product_id,
    case when count(asv) = 0 then
          '[]'::jsonb
    else
      json_agg((asv.name, asv.images)::export_albums)::jsonb
    end as albums
    from product_album_links as link
    left join albums as album on album.id = link.right_id
    left join album_search_view as asv on album.id = asv.album_id
    where link.left_id = new.id;

    return null;
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
         inner join product_album_links as link on (link.left_id = p.id)
      where link.id = new.id;
    when 'albums' then
      select array_agg(p.id) into product_ids
      from products as p
      inner join product_album_links as link on link.left_id = p.id
      inner join albums as album on (album.id = link.right_id)
      where album.id = new.id;
    when 'album_image_links' then
      select array_agg(p.id) into product_ids
      from products as p
      inner join product_album_links as link on link.left_id = p.id
      inner join albums as album on (album.id = link.right_id)
      inner join album_image_links as img_link on (img_link.left_id = album.id)
      where img_link.id = new.id;
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

create trigger insert_product_album_links_view_from_products
    after insert on products
    for each row
    execute procedure insert_product_album_links_view_from_products_fn();

create trigger update_product_album_links_view_from_products
  after update on products
  for each row
  execute procedure update_product_album_links_view_from_products_and_deps_fn();

create trigger update_product_album_links_view_from_product_album_links
  after update or insert on product_album_links
  for each row
  execute procedure update_product_album_links_view_from_products_and_deps_fn();

create trigger update_product_album_links_view_from_albums
  after update or insert on albums
  for each row
  execute procedure update_product_album_links_view_from_products_and_deps_fn();

create trigger update_product_album_links_view_from_album_image_links
  after update or insert on album_image_links
  for each row
  execute procedure update_product_album_links_view_from_products_and_deps_fn();

create trigger update_product_album_links_view_from_images
  after update or insert on images
  for each row
  execute procedure update_product_album_links_view_from_products_and_deps_fn();
