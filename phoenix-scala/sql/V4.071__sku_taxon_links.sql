
create table sku_taxon_links (
  id          serial primary key,
  left_id     integer not null references skus (id) on update restrict on delete restrict,
  right_id    integer not null references taxons (id) on update restrict on delete restrict,
  created_at  generic_timestamp,
  updated_at  generic_timestamp,
  archived_at generic_timestamp
);

create index sku_taxon_link_left_idx
  on sku_taxon_links (left_id);
create index sku_taxon_link_right_idx
  on sku_taxon_links (right_id);
