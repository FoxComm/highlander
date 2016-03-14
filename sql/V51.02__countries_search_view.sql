create materialized view countries_search_view as
select
	c.id,
	c.name,
	c.alpha2,
	c.alpha3,
	c.code,
	c.continent,
	c.currency,
	c.uses_postal_code,
	c.is_billable,
	c.is_shippable
from countries as c;

create unique index countries_search_view_idx on countries_search_view (id);
