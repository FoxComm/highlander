create table credit_cards (
    id integer primary key,
    -- parent_id: editing a CC creates a new version which points to the original parent.
    -- this allows us to reference a given CC for an order payment while preserving CC info used to charge
    -- the card as an immutable fact
    parent_id integer null references credit_cards(id) on update restrict on delete restrict,
    customer_id integer not null,
    billing_address_id integer not null,
    gateway_customer_id character varying(255) not null,
    gateway_card_id character varying(255) not null,
    holder_name character varying(255) not null,
    last_four character(4) not null,
    exp_month integer not null,
    exp_year integer not null,
    is_default boolean default false not null,
    -- in_wallet: controls whether or not we display this as CC in customer's wallet. it's false when CC
    -- has been deleted and false when deprecated by a versioned child.
    in_wallet boolean default true not null,
    created_at timestamp without time zone default (now() at time zone 'utc'),
    updated_at timestamp without time zone default (now() at time zone 'utc'),
    deleted_at timestamp without time zone null,
    -- address related fields
    region_id integer not null references regions(id) on update restrict on delete restrict,
    address_name character varying(255) not null,
    street1 character varying(255) not null,
    street2 character varying(255) null,
    city character varying(255) not null,
    zip character varying(12) not null,
    foreign key (id) references payment_methods(id) on update restrict on delete restrict,
    foreign key (customer_id) references customers(id) on update restrict on delete restrict,
    foreign key (billing_address_id) references addresses(id) on update restrict on delete restrict,
    constraint valid_last_four check (last_four ~ '[0-9]{4}'),
    constraint valid_exp_year check (exp_year between 2015 and 3000),
    constraint valid_exp_month check (exp_month between 1 and 12),
    constraint valid_zip check (zip ~ '(?i)^[a-z0-9][a-z0-9\- ]{0,10}[a-z0-9]$'),
);

create index credit_cards_customer_id_idx on credit_cards (customer_id);
create index credit_cards_in_wallet_idx on credit_cards (customer_id, in_wallet);

create unique index credit_cards_default_idx on credit_cards (customer_id, is_default, in_wallet)
    where is_default = true and in_wallet = true;

create trigger set_payment_method_id_trg
    before insert
    on credit_cards
    for each row
    execute procedure set_payment_method_id();

