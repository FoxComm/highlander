create table album_search_view
(
  album_id integer not null,
  context generic_string not null,
  context_id integer not null,
  name generic_string not null,
  images jsonb null default '[]',
  archived_at generic_string
);

create unique index album_search_view_idx on album_search_view (album_id, context);

create or replace function get_images_json_for_album(int) returns jsonb as $$
begin
    return json_agg(imgs) from (select 
            (form.attributes ->> (shadow.attributes -> 'src' ->> 'ref')) as src,
            (form.attributes ->> (shadow.attributes -> 'alt' ->> 'ref')) as alt,
            (form.attributes ->> (shadow.attributes -> 'title' ->> 'ref')) as title
    FROM images as image
    INNER JOIN album_image_links as lnk on (lnk.right_id = image.id)
    INNER JOIN object_forms as form on (form.id = image.form_id)
    INNER JOIN object_shadows as shadow on (shadow.id = image.shadow_id)
    WHERE lnk.left_id = $1        
    ORDER BY lnk.position) as imgs;
end;
$$ language plpgsql;

create or replace function insert_albums_view_from_albums_fn() returns trigger as $$
begin
  insert into album_search_view 
    select
      new.id as album_id,
      context.name as context,
      context.id as context_id,
      album_form.attributes ->> (album_shadow.attributes -> 'name' ->> 'ref') as name,
      get_images_json_for_album(new.id) as images,
      new.archived_at as archived_at
    from
      object_contexts as context
        left join object_shadows as album_shadow on (album_shadow.id = new.shadow_id)
        left join object_forms as album_form on (album_form.id = new.form_id)
    where
      context.id = new.context_id;

  return null;
end;
$$ language plpgsql;

create trigger insert_albums_view_from_albums
  after insert on albums
  for each row
  execute procedure insert_albums_view_from_albums_fn();

create or replace function update_albums_view_from_object_context_fn() returns trigger as $$
begin

  update album_search_view
    set
      context = subquery.name,
      context_id = subquery.id,
      archived_at = subquery.archived_at
    from
      (select
            o.id,
            o.name,
            albums.id as album_id,
            to_char(albums.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at
       from object_contexts as o
       inner join albums on (albums.context_id = o.id)
       where albums.context_id = new.id) as subquery
    where
      subquery.album_id = album_search_view.album_id;

  return null;
end;
$$ language plpgsql;

create trigger update_albums_view_from_object_contexts
  after update or insert on object_contexts
  for each row
  execute procedure update_albums_view_from_object_context_fn();

create or replace function update_albums_view_from_object_attrs_fn() returns trigger as $$
 declare album_ids int[];
 begin
   case tg_table_name
     when 'object_shadows' then
       select array_agg(albums.id) into strict album_ids
         from albums
         inner join object_shadows as album_shadow on (albums.shadow_id = album_shadow.id)
         where album_shadow.id = new.id;
     when 'object_forms' then
      select array_agg(albums.id) into strict album_ids
        from albums
        inner join object_forms as album_form on (albums.form_id = album_form.id)
        where album_form.id = new.id;
      when 'albums' then
        select array_agg(albums.id) into strict album_ids
          from albums
          where albums.id = new.id;
     when 'album_image_links' then
        select array_agg(new.left_id) into strict album_ids;
   end case;
 
   update album_search_view set
     name = subquery.name,
     images = subquery.images,
     archived_at = subquery.archived_at
     from (select
         album.id,
         album_form.attributes->>(album_shadow.attributes->'name'->>'ref') as name,
         get_images_json_for_album(album.id) as images,
         to_char(album.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"') as archived_at
       from albums as album
       inner join object_forms as album_form on (album_form.id = album.form_id)
       inner join object_shadows as album_shadow on (album_shadow.id = album.shadow_id)
       where album.id = any(album_ids)) as subquery
     where subquery.id = album_search_view.album_id;
 
   return null;
end;
$$ language plpgsql;

create trigger update_albums_view_from_object_shadows
  after update or insert on object_shadows
  for each row
  execute procedure update_albums_view_from_object_attrs_fn();

create trigger update_albums_view_from_object_forms
  after update or insert on object_forms
  for each row
  execute procedure update_albums_view_from_object_attrs_fn();

create trigger update_albums_view_from_albums
  after update on albums
  for each row
  execute procedure update_albums_view_from_object_attrs_fn();

create trigger update_albums_view_from_album_image_links
after update or insert on album_image_links
for each row
execute procedure update_albums_view_from_object_attrs_fn();
