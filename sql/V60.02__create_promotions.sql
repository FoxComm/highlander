create table promotions(
    id serial primary key,
    context_id integer not null references object_contexts(id) on update restrict on delete restrict,
    shadow_id integer not null references object_shadows(id) on update restrict on delete restrict,
    form_id integer not null references object_forms(id) on update restrict on delete restrict,
    commit_id integer references object_commits(id) on update restrict on delete restrict,
    apply_type generic_string not null,
    updated_at generic_timestamp,
    created_at generic_timestamp
);

create index promotions_apply_typx on promotions (apply_type);
create index promotions_object_context_idx on promotions (context_id);
create index promotions_promotion_form_idx on promotions (form_id);
