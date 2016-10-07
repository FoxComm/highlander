create or replace function update_albums_view_from_object_attrs_fn()
  returns trigger as $$
declare album_ids int [];
begin
  case tg_table_name
    when 'object_shadows'
    then
      select array_agg(albums.id) into strict album_ids
      from albums inner join object_shadows as album_shadow on (albums.shadow_id = album_shadow.id)
      where album_shadow.id = new.id;
    when 'object_forms'
    then
      select array_agg(albums.id) into strict album_ids
      from albums inner join object_forms as album_form on (albums.form_id = album_form.id)
      where album_form.id = new.id;
    when 'albums'
    then
      select array_agg(albums.id) into strict album_ids
      from albums where albums.id = new.id;
    when 'album_image_links'
    then
      case TG_OP
        when 'DELETE' then
          select array_agg(old.left_id) into strict album_ids;
      else
          select array_agg(new.left_id) into strict album_ids;
      end case;
  end case;

  update album_search_view
  set
    name        = subquery.name,
    images      = subquery.images,
    archived_at = subquery.archived_at
  from (select
          album.id,
          album_form.attributes ->> (album_shadow.attributes -> 'name' ->> 'ref') as name,
          get_images_json_for_album(album.id)                                     as images,
          to_char(album.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')             as archived_at
        from albums as album
          inner join object_forms as album_form on (album_form.id = album.form_id)
          inner join object_shadows as album_shadow on (album_shadow.id = album.shadow_id)
        where album.id = any (album_ids)) as subquery
  where subquery.id = album_search_view.album_id;

  return null;
end;
$$ language plpgsql;

drop trigger update_albums_view_from_album_image_links on album_image_links;

create trigger update_albums_view_from_album_image_links
  after update or insert or delete on album_image_links
for each row
execute procedure update_albums_view_from_object_attrs_fn();
