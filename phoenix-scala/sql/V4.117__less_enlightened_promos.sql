create table promotion_customer_group_links (
    id serial  primary key
  , left_id    integer not null references promotions(id)      on update restrict on delete restrict
  , right_id   integer not null references customer_groups(id) on update restrict on delete restrict
  , created_at generic_timestamp
  , updated_at generic_timestamp
  , foreign key (left_id)  references promotions(id)      on update restrict on delete restrict
  , foreign key (right_id) references customer_groups(id) on update restrict on delete restrict
  );

create index promotion_customer_group_link_left_idx  on promotion_customer_group_links (left_id);
create index promotion_customer_group_link_right_idx on promotion_customer_group_links (right_id);

insert into promotion_customer_group_links
  ( left_id
  , right_id
  , created_at
  , updated_at
  )
  select
      promotions.id
    , jsonb_array_elements_text(
        illuminate_obj(object_forms, object_shadows, 'customerGroupIds')
      ) :: integer
    , now()
    , now()
  from
    promotions
    inner join object_forms   on (object_forms.id   = promotions.form_id)
    inner join object_shadows on (object_shadows.id = promotions.shadow_id)
  ;

-------------------------------------------------------------------------------

alter table promotions
    add column name text
  , add column active_from timestamp
  , add column active_to timestamp
  ;

update promotions
set
    name = q.name
  , active_from = q.active_from
  , active_to = q.active_to
from
  (select
      promotions.id
    , illuminate_obj(object_forms, object_shadows, 'name') as name
    , illuminate_text(object_forms, object_shadows, 'activeFrom') :: timestamp as active_from
    , illuminate_text(object_forms, object_shadows, 'activeTo') :: timestamp as active_to
  from
    promotions
    inner join object_forms   on (object_forms.id   = promotions.form_id)
    inner join object_shadows on (object_shadows.id = promotions.shadow_id)
  ) as q
where
  promotions.id = q.id
  ;

update object_shadows
set
    attributes = q.attributes
from
  (select
      object_shadows.id as id
    , object_shadows.attributes
        - 'name'
        - 'details'
        - 'description'
        - 'storefrontName'
        - 'customerGroupIds'
        - 'activeFrom'
        - 'activeTo'
        as attributes
  from
    object_shadows
    inner join promotions on (object_shadows.id = promotions.shadow_id)
  ) as q
where
  object_shadows.id = q.id;
  ;
