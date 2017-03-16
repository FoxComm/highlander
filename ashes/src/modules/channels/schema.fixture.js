module.exports = {
  "$schema": "http://json-schema.org/draft-04/schema#",
  "title": "amazon_clothes_product",
  "type": "object",
  "properties": {
    "attributes": {
      "type": "object",
      "properties": {
        "category_id" : {
          "title": "Amazon Category ID",
          "disabled": true,
          "type": ["string", "null"]
        },
        "title": {
          "type": ["string", "null"]
        },
        "description": {
          "type": "string",
          "widget": "richText"
        },
        // "taxCode" : {
        //   "type": ["string", "A_GEN_NOTAX"]
        // },
        "brand" : {
          "type": ["string", "null"]
        },
        "manufacturer" : {
          "type": ["string", "null"]
        },
        "bulletPoint1" : {
          "type": ["string", "null"]
        },
        "bulletPoint2" : {
          "type": ["string", "null"]
        },
        "bulletPoint3" : {
          "type": ["string", "null"]
        },
        "bulletPoint4" : {
          "type": ["string", "null"]
        },
      },
      "required": ["code", "brand", "bulletPoint1", "bulletPoint2", "bulletPoint3", "bulletPoint4"]
    }
  }
};
