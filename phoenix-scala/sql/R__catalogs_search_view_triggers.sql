create or replace function catalogs_search_view_insert_fn() returns trigger as $$
begin
  insert into catalogs_search_view(
    id,
    scope,
    name,
    site,
    country_id,
    country_name,
    default_language,
    created_at,
    updated_at) select distinct on (new.id)
        catalog.id as id,
        catalog.scope as scope,
        catalog.name as name,
        catalog.site as site,
        country.id as country_id,
        country.name as country_name,
        catalog.default_language as default_language,
        to_json_timestamp(catalog.created_at) as created_at,
        to_json_timestamp(catalog.updated_at) as updated_at
      from catalogs as catalog
        inner join countries as country on (country.id = catalog.country_id)
      where new.id = catalog.id;
  return null;
end;
$$ language plpgsql;

create or replace function catalogs_search_view_update_fn() returns trigger as $$
begin
  update catalogs_search_view
    set
      name = catalog.name,
      site = catalog.site,
      country_id = country.id,
      country_name = country.name,
      default_language = catalog.default_language,
      updated_at = to_char(catalog.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
    from catalogs as catalog
      inner join countries as country on (country.id = catalog.country_id)
    where catalog.id = new.id and catalogs_search_view.id = new.id;
  return null;
end;
$$ language plpgsql;

drop trigger if exists catalogs_search_view_insert on catalogs;
create trigger catalogs_search_view_insert
  after insert on catalogs
  for each row
  execute procedure catalogs_search_view_insert_fn();

drop trigger if exists catalogs_search_view_update on catalogs;
create trigger catalogs_search_view_update
  after update on catalogs
  for each row
  execute procedure catalogs_search_view_update_fn();
