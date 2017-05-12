create or replace function insert_product_reviews_search_view_from_insert_fn()
  returns trigger
as $$
begin
  insert into product_reviews_search_view select
                                            pr.id,
                                            pr.scope,
                                            skus.code,
                                            users.name,
                                            pr.content -> 'title' ->> 'v' as title,
                                            pr.content -> 'body' ->> 'v' as body,
                                            pr.created_at,
                                            pr.updated_at,
                                            pr.archived_at
                                          from product_reviews as pr
                                            inner join users on pr.user_id = users.id
                                            inner join skus on pr.sku_id = skus.id;

  return null;
end; $$ language plpgsql;

drop trigger if exists insert_product_reviews_search_view_from_insert on product_reviews;
create trigger insert_product_reviews_search_view_from_insert
after insert on product_reviews
for each row
execute procedure insert_product_reviews_search_view_from_insert_fn();

create function update_product_reviews_search_view_from_update_fn()
  returns trigger
as $$
begin
  update product_reviews_search_view
  set title     = new.content -> 'title' ->> 'v',
    body        = new.content -> 'body' ->> 'v',
    updated_at  = new.updated_at,
    archived_at = new.archived_at
  where product_reviews_search_view.id = new.id;

  return null;
end; $$ language plpgsql;

drop trigger if exists update_product_reviews_search_view_from_update on product_reviews;
create trigger update_product_reviews_search_view_from_update
after update on product_reviews
for each row
execute procedure update_product_reviews_search_view_from_update_fn();


create function update_product_reviews_search_view_from_user_update_fn()
  returns trigger
as $$
begin
  update product_reviews_search_view
  set user_name = new.name
  where product_reviews_search_view.id in (select product_reviews.id where user_id = new.id);
  return null;
end; $$ language plpgsql;

drop trigger if exists update_product_reviews_search_view_from_user_update on users;
create trigger update_product_reviews_search_view_from_user_update
after update on users
for each row when (new.name is distinct from old.name)
execute procedure update_product_reviews_search_view_from_user_update_fn();


create function update_product_reviews_search_view_from_sku_update_fn()
  returns trigger
as $$
begin
  update product_reviews_search_view
  set sku = new.code
  where product_reviews_search_view.id in (select product_reviews.id where sku_id = new.id);
end; $$ language plpgsql;

drop trigger if exists update_product_reviews_search_view_from_sku_update on skus;
create trigger update_product_reviews_search_view_from_sku_update
after update on skus
for each row when (new.code is distinct from old.code)
execute procedure update_product_reviews_search_view_from_sku_update_fn();
