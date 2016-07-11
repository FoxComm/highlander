create materialized view regions_search_view as
select
	r.id,
	r.name,
	r.abbreviation,
    r.country_id
from regions as r;

create unique index regions_search_view_idx on regions_search_view (id);
