create or replace function insert_variant_album_links_view_from_variants_fn() returns trigger as $$
begin
  insert into variant_album_links_view select
    new.id as product_variant_id,
    case when count(asv) = 0 then
      '[]'::jsonb
    else
      json_agg((asv.name, asv.images)::export_albums)::jsonb
    end as albums
    from variant_album_links as link
    left join albums as album on album.id = link.right_id
    left join album_search_view as asv on album.id = asv.album_id
    where link.left_id = new.id;

    return null;
end;
$$ language plpgsql;

create or replace function get_albums_json_for_variant(int) returns jsonb as $$
begin
  return case when count(albums) = 0
         then '[]'
         else json_agg(albums)
         end from (select asv.name as "name", asv.images as "images"
    from album_search_view as asv
      inner join variant_album_links as link on asv.album_id = link.right_id
  where link.left_id = $1
  order by link.position) as albums;
end;
$$ language plpgsql;

create or replace function update_variant_album_links_view_from_variants_and_deps_fn() returns trigger as $$
declare variant_ids int[];
begin
  case tg_table_name
    when 'product_variants' then
      variant_ids := array_agg(new.id);
    when 'variant_album_links' then
      select array_agg(pv.id) into variant_ids
      from product_variants as pv
      where pv.id = (case TG_OP
                     when 'DELETE' then old.left_id
                       else new.left_id
                       end);
    when 'albums' then
      select array_agg(pv.id) into variant_ids
      from product_variants as pv
      inner join variant_album_links as link on link.left_id = pv.id
      inner join albums as album on (album.id = link.right_id)
      where album.id = new.id;
    when 'album_search_view' then
      select array_agg(pv.id) into variant_ids
      from product_variants as pv
      inner join variant_album_links as link on link.left_id = pv.id
      inner join albums on (albums.id = link.right_id)
      where albums.id = new.album_id;
    when 'images' then
      select array_agg(pv.id) into variant_ids
      from product_variants as pv
      inner join variant_album_links as link on link.left_id = pv.id
      inner join albums as album on (album.id = link.right_id)
      inner join album_image_links as img_link on (img_link.left_id = album.id)
      inner join images as image on (image.id = img_link.right_id)
      where image.id = new.id;
    else
      variant_ids := '{}';
  end case;

  update variant_album_links_view set
    albums = subquery.albums
  from (select
          pv.id                              as product_variant_id,
          get_albums_json_for_variant(pv.id) as albums
        from product_variants as pv
        where pv.id = any(variant_ids)) as subquery
    where subquery.product_variant_id = variant_album_links_view.product_variant_id;
    return null;
end;
$$ language plpgsql;

drop trigger if exists insert_variant_album_links_view_from_variants on product_variants;
create trigger insert_variant_album_links_view_from_variants
    after insert on product_variants
    for each row
    execute procedure insert_variant_album_links_view_from_variants_fn();

drop trigger if exists update_variant_album_links_view_from_variants on product_variants;
create trigger update_variant_album_links_view_from_variants
  after update on product_variants
  for each row
  execute procedure update_variant_album_links_view_from_variants_and_deps_fn();

drop trigger if exists update_variant_album_links_view_from_variant_album_links on variant_album_links;
create trigger update_variant_album_links_view_from_variant_album_links
  after update or insert on variant_album_links
  for each row
  execute procedure update_variant_album_links_view_from_variants_and_deps_fn();

drop trigger if exists update_variant_album_links_view_from_albums on albums;
create trigger update_variant_album_links_view_from_albums
  after update or insert on albums
  for each row
  execute procedure update_variant_album_links_view_from_variants_and_deps_fn();

drop trigger if exists update_variant_album_links_view_from_album_image_links on album_image_links;
create trigger update_variant_album_links_view_from_album_image_links
  after update or insert on album_image_links
  for each row
  execute procedure update_variant_album_links_view_from_variants_and_deps_fn();

drop trigger if exists update_variant_album_links_view_from_images on images;
create trigger update_variant_album_links_view_from_images
  after update or insert on images
  for each row
  execute procedure update_variant_album_links_view_from_variants_and_deps_fn();
