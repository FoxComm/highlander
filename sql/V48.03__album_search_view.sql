create table album_search_view
(
  id integer not null,
  context generic_string not null,
  context_id integer not null,
  name generic_string not null,
  images text null default '[]'
);
create unique index album_search_view_idx on album_search_view (id, context);

create or replace function insert_albums_view_from_albums_fn() returns trigger as $$
begin

  insert into album_search_view 
    select
      NEW.id as id,
      context.name as context,
      context.id as context_id,
      album_form.attributes->>(album_shadow.attributes->'name'->>'ref') as name,
      album_form.attributes->>(album_shadow.attributes->'images'->>'ref') as images
    from
      object_contexts as context
        left join object_shadows as album_shadow on (album_shadow.id = NEW.shadow_id)
        left join object_forms as album_form on (album_form.id = NEW.form_id)
    where
      context.id = NEW.context_id;

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
      context_id = subquery.id
    from
      (select o.id, o.name, albums.id as album_id
       from object_contexts as o
       inner join albums on (albums.context_id = o.id)
       where albums.context_id = NEW.id) as subquery
    where
      subquery.album_id = album_search_view.id;

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
   case TG_TABLE_NAME
     when 'object_shadows' then
       select array_agg(albums.id) into strict album_ids
         from albums
         inner join object_shadows as album_shadow on (albums.shadow_id = album_shadow.id)
         where album_shadow.id = NEW.id;
     when 'object_forms' then
      select array_agg(albums.id) into strict album_ids
        from albums
        inner join object_forms as album_form on (albums.form_id = album_form.id)
        where album_form.id = NEW.id;
   end case;
 
   update album_search_view set
     name = subquery.name,
     images = subquery.images
     from (select
         album.id,
         album_form.attributes->>(album_shadow.attributes->'name'->>'ref') as name,
         album_form.attributes->(album_shadow.attributes->'images'->>'ref') as images
       from albums as album
       inner join object_forms as album_form on (album_form.id = album.form_id)
       inner join object_shadows as album_shadow on (album_shadow.id = album.shadow_id)
       where album.id = ANY(album_ids)) as subquery
     where subquery.id = album_search_view.id;
 
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
