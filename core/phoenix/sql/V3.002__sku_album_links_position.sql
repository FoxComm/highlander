alter table sku_album_links
add column "position" integer not null default 0;

create index sku_album_links_position on sku_album_links (left_id, "position")

