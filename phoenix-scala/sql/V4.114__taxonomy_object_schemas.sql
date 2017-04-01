
insert into object_schemas (kind, name, schema, created_at)
select 'taxonomy', 'taxonomy', '
{
    "$schema": "http://json-schema.org/draft-04/schema#",
    "title": "Taxonomy",
    "type": "object",
    "properties": {
        "attributes": {
          "type": "object",
          "properties": {
            "name": {
              "type": "string",
              "minLength": 1
            },
            "activeTo": {
              "type": [
                "string",
                "null"
              ],
              "format": "date-time"
            },
            "activeFrom": {
              "type": [
                "string",
                "null"
              ],
              "format": "date-time"
            },
            "description": {
              "type": "string",
              "widget": "richText"
            }
          },
          "required": ["name"],
          "description": "Taxonomy attributes itself"
        },
        "hierarchical": {
          "type": "boolean"
        }
    }
}
', now()
where not exists ( select id from object_schemas where kind = 'taxonomy' AND name = 'taxonomy' );

insert into object_schemas (kind, name, schema, created_at)
select 'taxon', 'taxon', '
{
    "$schema": "http://json-schema.org/draft-04/schema#",
    "title": "Taxonomy",
    "type": "object",
    "properties": {
      "attributes": {
        "type": "object",
        "properties": {
          "name": {
            "type": "string",
            "minLength": 1
          },
          "activeTo": {
            "type": [
              "string",
              "null"
            ],
            "format": "date-time"
          },
          "activeFrom": {
            "type": [
              "string",
              "null"
            ],
            "format": "date-time"
          },
          "description": {
            "type": "string",
            "widget": "richText"
          }
        },
        "required": ["name"],
        "description": "Taxon attributes"
      },
      "taxonomyId": {
        "type": "number"
      },
      "parentId": {
        "type": "number"
      }
    }
}
', now()
where not exists ( select id from object_schemas where kind = 'taxon' AND name = 'taxon' );
