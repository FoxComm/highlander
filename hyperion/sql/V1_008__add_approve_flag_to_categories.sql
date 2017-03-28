alter table amazon_categories add column approve_needed boolean;
update amazon_categories set approve_needed=true;