create table sellable_inventory_summaries (
  id serial primary key,
  on_hand integer not null default 0 check (on_hand >= 0),
  on_hold integer not null default 0 check (on_hold >= 0),
  reserved integer not null default 0 check (reserved >= 0),
  safety_stock integer not null default 0 check (safety_stock >= 0),
  available_for_sale integer not null default 0 check (available_for_sale >= 0),
  updated_at timestamp without time zone default (now() at time zone 'utc')
);

create table preorder_inventory_summaries (
  id serial primary key,
  on_hand integer not null default 0 check (on_hand >= 0),
  on_hold integer not null default 0 check (on_hold >= 0),
  reserved integer not null default 0 check (reserved >= 0),
  available_for_sale integer not null default 0 check (available_for_sale >= 0),
  updated_at timestamp without time zone default (now() at time zone 'utc')
);

create table backorder_inventory_summaries (
  id serial primary key,
  on_hand integer not null default 0 check (on_hand >= 0),
  on_hold integer not null default 0 check (on_hold >= 0),
  reserved integer not null default 0 check (reserved >= 0),
  available_for_sale integer not null default 0 check (available_for_sale >= 0),
  updated_at timestamp without time zone default (now() at time zone 'utc')
);

create table nonsellable_inventory_summaries (
  id serial primary key,
  on_hand integer not null default 0 check (on_hand >= 0),
  on_hold integer not null default 0 check (on_hold >= 0),
  reserved integer not null default 0 check (reserved >= 0),
  available_for_sale integer not null default 0 check (available_for_sale >= 0),
  updated_at timestamp without time zone default (now() at time zone 'utc')
);

create function afs(on_hand integer, on_hold integer, reserved integer, safety_stock integer default 0) returns integer as $$
begin
  return on_hand - on_hold - reserved - safety_stock;
end;
$$ language plpgsql;

create function update_afs() returns trigger as $$
begin
  new.available_for_sale = afs(new.on_hand, new.on_hold, new.reserved);
  return new;
end;
$$ language plpgsql;

create function update_afs_sell() returns trigger as $$
begin
  new.available_for_sale = afs(new.on_hand, new.on_hold, new.reserved, new.safety_stock);
  return new;
end;
$$ language plpgsql;

create trigger update_afs_sellable
  before insert or update on sellable_inventory_summaries
  for each row execute procedure update_afs_sell();

create trigger update_afs_preorder
  before insert or update on preorder_inventory_summaries
  for each row execute procedure update_afs();

create trigger update_afs_backorder
  before insert or update on backorder_inventory_summaries
  for each row execute procedure update_afs();

create trigger update_afs_nonsellable
  before insert or update on nonsellable_inventory_summaries
  for each row execute procedure update_afs();
