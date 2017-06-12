create or replace function insert_product_reviews_search_view_from_insert_fn()
  returns trigger
as $$
begin
  insert into product_reviews_search_view select
                                            pr.id,
                                            pr.scope,
                                            skus.code,
                                            users.name,
                                            users.account_id,
                                            pr.content -> 'title' ->> 'v' as title,
                                            cast(pr.content -> 'rating' ->> 'v' as integer) as rating ,
                                            pr.content,
                                            to_char(pr.created_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
                                            to_char(pr.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
                                            to_char(pr.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
                                          from product_reviews as pr
                                            inner join users on pr.user_id = users.account_id
                                            inner join skus on pr.sku_id = skus.id
                                          where pr.id = new.id;

  return null;
end; $$ language plpgsql;

drop trigger if exists insert_product_reviews_search_view_from_insert on product_reviews;
create trigger insert_product_reviews_search_view_from_insert
after insert on product_reviews
for each row
execute procedure insert_product_reviews_search_view_from_insert_fn();

create or replace function update_product_reviews_search_view_from_update_fn()
  returns trigger
as $$
begin
  update product_reviews_search_view
  set
    title       = new.content -> 'title' ->> 'v',
    rating      = cast(new.content -> 'rating' ->> 'v' as integer),
    attributes  = new.content,
    updated_at  = to_char(new.updated_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"'),
    archived_at = to_char(new.archived_at, 'YYYY-MM-DD"T"HH24:MI:SS.MS"Z"')
  where product_reviews_search_view.id = new.id;

  return null;
end; $$ language plpgsql;

drop trigger if exists update_product_reviews_search_view_from_update on product_reviews;
create trigger update_product_reviews_search_view_from_update
after update on product_reviews
for each row
execute procedure update_product_reviews_search_view_from_update_fn();

drop trigger if exists update_product_reviews_search_view_from_user_update_fn on product_reviews;
create or replace function update_product_reviews_search_view_from_user_update_fn()
  returns trigger
as $$
begin
  update product_reviews_search_view
  set user_name = new.name
  where product_reviews_search_view.id in (select id from product_reviews where user_id = new.id);
  return null;
end; $$ language plpgsql;

drop trigger if exists update_product_reviews_search_view_from_user_update on users;
create trigger update_product_reviews_search_view_from_user_update
after update on users
for each row when (new.name is distinct from old.name)
execute procedure update_product_reviews_search_view_from_user_update_fn();

create or replace function update_product_reviews_search_view_from_sku_update_fn()
  returns trigger
as $$
begin
  update product_reviews_search_view
  set sku = new.code
  where product_reviews_search_view.id in (select id from product_reviews where sku_id = new.id);
  return null;
end; $$ language plpgsql;

drop trigger if exists update_product_reviews_search_view_from_sku_update on skus;
create trigger update_product_reviews_search_view_from_sku_update
after update on skus
for each row when (new.code is distinct from old.code)
execute procedure update_product_reviews_search_view_from_sku_update_fn();
