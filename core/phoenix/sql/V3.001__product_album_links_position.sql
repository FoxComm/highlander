alter table product_album_links
add column "position" integer not null default 0;

create index product_album_links_position on product_album_links (left_id, "position")

