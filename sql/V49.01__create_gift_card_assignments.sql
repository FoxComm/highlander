create table gift_card_assignments (
    id serial primary key,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    gift_card_id integer not null references gift_cards(id) on update restrict on delete restrict,
    assignee_id integer not null references store_admins(id) on update restrict on delete restrict
)