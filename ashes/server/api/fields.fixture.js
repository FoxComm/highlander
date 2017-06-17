module.exports = {
  $schema: 'http://json-schema.org/draft-04/schema#',
  title: 'amazon_clothes_product',
  type: 'object',
  properties: {
    attributes: {
      type: 'object',
      properties: {
        code: {
          title: 'SKU',
          type: 'string',
          minLength: 1,
        },
        title: {
          type: ['string', 'null'],
        },
        upc: {
          title: 'UPC',
          type: 'string',
        },
        category: {
          type: ['hidden', 'clothes'],
        },
        taxCode: {
          type: ['string', 'A_GEN_NOTAX'],
        },
        brand: {
          type: ['string', 'null'],
        },
        manufacturer: {
          type: ['string', 'null'],
        },
        bulletPoint1: {
          type: ['string', 'null'],
        },
        bulletPoint2: {
          type: ['string', 'null'],
        },
        bulletPoint3: {
          type: ['string', 'null'],
        },
        bulletPoint4: {
          type: ['string', 'null'],
        },
        department: {
          type: ['hidden', 'null'],
        },
        itemType: {
          type: ['hidden', 'null'],
        },
        description: {
          type: 'string',
          widget: 'richText',
        },
        retailPrice: {
          type: 'object',
          widget: 'price',
          properties: {
            currency: {
              type: 'string',
              minLength: 3,
              maxLength: 3,
            },
            value: {
              type: 'number',
              minimum: 0,
              default: 0,
            },
          },
        },
        activeFrom: {
          type: ['string', 'null'],
          format: 'date-time',
        },
        activeTo: {
          type: ['string', 'null'],
          format: 'date-time',
        },
      },
      required: ['code', 'brand', 'bulletPoint1', 'bulletPoint2', 'bulletPoint3', 'bulletPoint4', 'retailPrice'],
    },
  },
};
