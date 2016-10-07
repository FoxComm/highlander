drop materialized view promotions_search_view;
drop materialized view promotion_discount_links_view;

create table promotion_discount_links_view
(
    promotion_id bigint not null unique,
    context_id integer,
    discounts jsonb not null default '[]'::jsonb
);

create table promotions_search_view
(
    id bigint,
    context generic_string,
    apply_type generic_string,
    promotion_name text,
    storefront_name text,
    description text,
    active_from json_timestamp,
    active_to json_timestamp,
    total_used integer,
    current_carts integer,
    created_at json_timestamp,
    archived_at json_timestamp,
    discounts jsonb not null default '[]'::jsonb
);

create unique index promotions_search_view_idx on promotions_search_view (id, context);

-- discounts INSERT
create or replace function update_promotions_disc_links_view_insert_fn() returns trigger as $$
  begin
    insert into promotion_discount_links_view select distinct on (new.id)
        p.form_id as promotion_id,
        context.id as context_id,
        -- discounts
        case when count(discount) = 0
          then
            '[]'::jsonb
        else
            json_agg(discount.form_id)::jsonb
        end as discounts
      from promotions as p
      inner join object_contexts as context on (p.context_id = context.id)
      inner join discounts as discount on (discount.id = new.right_id)
      where p.id = new.left_id
      group by p.form_id, context.id;

      return null;
  end;
$$ language plpgsql;

create trigger update_promotions_disc_links_view_insert
    after insert on promotion_discount_links
    for each row
    execute procedure update_promotions_disc_links_view_insert_fn();

-- discounts update

create or replace function update_promotions_disc_links_view_update_fn() returns trigger as $$
declare prom_ids text[];
begin
  case tg_table_name
    when 'promotion_discount_links' then
      select array_agg(p.form_id) into strict prom_ids
      from promotions as p
        where p.id = new.left_id;
    when 'discounts' then
      select array_agg(p.form_id) into strict prom_ids
      from promotions as p
        inner join promotion_discount_links as link on (p.id = link.left_id)
        where new.id = link.right_id;
  end case;

  update promotion_discount_links_view set
    promotion_id = q.promotion_id,
    discounts = q.discounts
    from (select
    p.form_id as promotion_id,
    -- discounts
    case when count(discount) = 0
      then
        '[]'::jsonb
    else
        json_agg(discount.form_id)::jsonb
    end as discounts

    from promotions as p
      inner join object_contexts as context on (p.context_id = context.id)
      inner join promotion_discount_links as link on (link.left_id = p.id)
      inner join discounts as discount on (discount.id = link.right_id)
      where p.form_id = any(prom_ids)
      group by p.form_id, context.id) as q
    where promotion_discount_links_view.promotion_id = q.promotion_id;

    return null;
end;
$$ language plpgsql;


create trigger update_promotions_disc_links_view_update
  after update on promotion_discount_links
  for each row
  execute procedure update_promotions_disc_links_view_update_fn();

create trigger update_promotions_disc_links_view_update
  after update on discounts
  for each row
  execute procedure update_promotions_disc_links_view_update_fn();