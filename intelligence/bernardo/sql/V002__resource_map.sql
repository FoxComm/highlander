
--The resource map maps a resource to a set of resources. 
--The set of resources is one or more resources the river rock proxy can select.
-- if mapped is a string, then the mapping is direct and there is no selection.
-- if mapped is an array of string, then river rock will select based on stats
--    about the mapped resource
-- if mapped is a json object then river rock will most of the time
--    select the prefered resource, otherwise will select the remaining
--    The json object should look like this
--    {
--      "pref": <resource>
--      "alt" : [<resource>, ...]
--    }
--     

create table resource_map (
    id serial primary key,
    cluster_id integer references clusters(id),
    res text,
    mapped jsonb
);
