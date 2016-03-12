create table skus(
    id serial primary key,
    code generic_string not null,
    context_id integer not null references sku_contexts(id) on update restrict on delete restrict,
    shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
    form_id integer not null references object_forms(id) on update restrict on delete restrict,
    commit_id integer references object_commits(id) on update restrict on delete restrict,
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    created_at timestamp without time zone default (now() at time zone 'utc')
);

create index sku_heads_codex on sku_heads (code);
create index sku_heads_sku_context_idx on sku_heads (context_id);

