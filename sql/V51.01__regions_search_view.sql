create materialized view regions_search_view as
select
	r.id,
	r.name,
	r.abbreviation,
	-- Country
	to_json((
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
	)::export_countries) as country
from regions as r
inner join countries as c on (r.country_id = c.id);

create unique index regions_search_view_idx on regions_search_view (id);
