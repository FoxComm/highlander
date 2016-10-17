create table taxonomies (
  id           serial primary key,
  scope        exts.ltree not null,
  hierarchical boolean not null,
  context_id   integer not null references object_contexts (id) on update restrict on delete restrict,
  shadow_id    integer not null references object_shadows (id) on update restrict on delete restrict,
  form_id      integer not null references object_forms (id) on update restrict on delete restrict,
  commit_id    integer references object_commits (id) on update restrict on delete restrict,
  updated_at   generic_timestamp,
  created_at   generic_timestamp,
  archived_at  generic_timestamp
);

create table taxons (
  id          serial primary key,
  scope       exts.ltree not null,
  context_id  integer not null references object_contexts (id) on update restrict on delete restrict,
  shadow_id   integer not null references object_shadows (id) on update restrict on delete restrict,
  form_id     integer not null references object_forms (id) on update restrict on delete restrict,
  commit_id   integer references object_commits (id) on update restrict on delete restrict,
  updated_at  generic_timestamp,
  created_at  generic_timestamp,
  archived_at generic_timestamp
);

create table taxonomy_taxon_links (
  id          serial primary key,
  index       integer not null,
  taxonomy_id integer not null references taxonomies (id),
  taxon_id    integer not null references taxons (id),
  position    integer not null,
  path        exts.ltree   not null,
  updated_at  generic_timestamp,
  created_at  generic_timestamp,
  archived_at generic_timestamp
);

create index taxonomy_taxon_link_taxonomy_idx
  on taxonomy_taxon_links (taxonomy_id);
create index taxonomy_taxon_link_term_idx
  on taxonomy_taxon_links (taxon_id);

create unique index taxonomy_taxon_link_index_idx
on taxonomy_taxon_links (taxonomy_id, index)
  where archived_at is null ;

create table product_taxonomy_links (
  id          serial primary key,
  left_id     integer not null references products (id) on update restrict on delete restrict,
  right_id    integer not null references taxons (id) on update restrict on delete restrict,
  created_at  generic_timestamp,
  updated_at  generic_timestamp,
  archived_at generic_timestamp
);

create index product_taxonomy_link_left_idx
  on product_taxonomy_links (left_id);
create index product_taxonomy_link_right_idx
  on product_taxonomy_links (right_id);

create function archive_taxons_if_taxonomy_is_archived_fn() returns trigger as $$
begin
  update taxons
  set archived_at = now()
  where taxons.id in (
    select taxonomy_taxon_links.taxon_id
    from taxonomy_taxon_links
    where taxonomy_id = NEW.id and
      taxonomy_taxon_links.archived_at is null);

  update taxonomy_taxon_links
  set archived_at = now()
  where taxonomy_id = NEW.id and archived_at is null;
  return null;
end;
$$ language plpgsql;

create trigger archive_taxons_after_taxonomy
after update on taxonomies
for each row
when (new.archived_at is not null and old.archived_at is null)
execute procedure archive_taxons_if_taxonomy_is_archived_fn();
