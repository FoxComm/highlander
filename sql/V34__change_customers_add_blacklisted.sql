alter table customers
    add column blacklisted boolean not null default false,
    add column blacklisted_by integer,
    add column blacklisted_reason generic_string;