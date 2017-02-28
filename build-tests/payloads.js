import path from 'path';
import faker from 'faker';
import testImageBase64 from './assets/image.base64';

const $ = {
  randomNumber: (min, max) => faker.random.number({ min, max }),
  randomMonth: () => $.randomNumber(1, 12),
  randomYear: () => $.randomNumber(2020, 2025),
  randomArrayElement: array => faker.random.arrayElement(array),
  adminName: 'Frankly Admin',
  adminEmail: 'admin@admin.com',
  adminPassword: 'password',
  adminOrg: 'tenant',
  customerOrg: 'merchant',
  testCardNumber: '4242424242424242',
  testImagePath: path.resolve('./assets/image.jpg'),
  randomUserCredentials: () => ({
    email: `${Date.now()}@bvt.com`,
    name: faker.name.findName(),
    password: faker.internet.password(),
  }),
  randomCreateAddressPayload: () => ({
    name: faker.name.findName(),
    regionId: 4123,
    address1: faker.address.streetAddress(),
    address2: faker.address.secondaryAddress(),
    city: faker.address.city(),
    zip: faker.address.zipCode().replace('-', ''),
    isDefault: false,
    phoneNumber: faker.phone.phoneNumber('##########'),
  }),
  randomCreditCardDetailsPayload: customerId => ({
    customerId,
    cardNumber: $.testCardNumber,
    expMonth: $.randomMonth(),
    expYear: $.randomYear(),
    cvv: 100 + $.randomNumber(0, 899),
    address: $.randomCreateAddressPayload(),
  }),
  randomCreditCardCreatePayload: () => ({
    holderName: faker.name.findName(),
    number: $.testCardNumber,
    cvv: '###'.replace(/#/g, $.randomNumber(0, 9).toString()),
    expMonth: $.randomMonth(),
    expYear: $.randomYear(),
    address: $.randomCreateAddressPayload(),
    isDefault: false,
  }),
  randomStoreCreditPayload: () => ({
    amount: $.randomNumber(1000, 10000),
    reasonId: 1,
    subReasonId: 1,
  }),
  randomCreateNotePayload: () => ({
    body: faker.lorem.sentence(),
  }),
  randomUpdateNotePayload: () => ({
    body: faker.lorem.sentence(),
  }),
  randomSkuPayload: () => {
    const now = Date.now();
    return {
      attributes: {
        code: {
          t: 'string',
          v: `SKU-BVT-${now}`,
        },
        upc: {
          t: 'string',
          v: `SKU-BVT-${now} UPC`,
        },
        title: {
          t: 'string',
          v: `SKU-BVT-${now} T`,
        },
        description: {
          t: 'richText',
          v: `<p>${faker.lorem.sentence()}</p>`,
        },
        activeFrom: {
          t: 'datetime',
          v: '2016-10-31T15:13:15.799Z',
        },
        activeTo: {
          t: 'datetime',
          v: null,
        },
        unitCost: {
          t: 'price',
          v: {
            currency: 'USD',
            value: 0,
          },
        },
        salePrice: {
          t: 'price',
          v: {
            currency: 'USD',
            value: 4800,
          },
        },
        retailPrice: {
          t: 'price',
          v: {
            currency: 'USD',
            value: 5200,
          },
        },
      },
    };
  },
  randomProductPayload: ({ minSkus, maxSkus } = {}) => {
    const now = Date.now();
    return {
      attributes: {
        metaDescription: {
          t: 'string',
          v: null,
        },
        metaTitle: {
          t: 'string',
          v: null,
        },
        url: {
          t: 'string',
          v: null,
        },
        description: {
          t: 'richText',
          v: `<p>${faker.lorem.sentence()}</p>`,
        },
        title: {
          t: 'string',
          v: `P-BVT-${now}`,
        },
        tags: {
          t: 'tags',
          v: ['bvt'],
        },
        activeFrom: {
          v: '2016-07-27T23:47:27.518Z',
          t: 'datetime',
        },
        activeTo: {
          v: null,
          t: 'datetime',
        },
      },
      skus: Array
        .from({ length: $.randomNumber(minSkus || 1, maxSkus || 1) })
        .map(() => $.randomSkuPayload()),
    };
  },
  randomGiftCardPayload: () => ({
    balance: $.randomNumber(1, 2000),
    quantity: 1,
    reasonId: 1,
  }),
  randomImagePayload: () => ({
    src: faker.random.arrayElement([
      'https://s3-us-west-1.amazonaws.com/foxcomm-images/albums/1/540/image.jpg',
      testImageBase64,
    ]),
    alt: faker.lorem.word(),
    title: faker.lorem.word(),
  }),
  randomAlbumPayload: ({ minImages, maxImages } = {}) => {
    const images = [];
    const imageCount = $.randomNumber(minImages || 0, maxImages || 3);
    for (let i = 0; i < imageCount; i += 1) {
      images.push($.randomImagePayload());
    }
    return {
      name: `A-${Date.now()}`,
      images,
    };
  },
  randomCreateDiscountPayload: () => ({
    attributes: {
      qualifier: {
        t: 'qualifier',
        v: {
          orderAny: {},
        },
      },
      offer: {
        t: 'offer',
        v: {
          orderPercentOff: {
            discount: $.randomNumber(1, 99),
          },
        },
      },
    },
  }),
  randomCreatePromotionPayload: () => {
    const now = Date.now();
    return {
      attributes: {
        name: {
          t: 'string',
          v: `PROMO-${now}`,
        },
        storefrontName: {
          t: 'richText',
          v: `<p>PROMO-${now}</p>`,
        },
        description: {
          t: 'text',
          v: faker.lorem.sentence(),
        },
        details: {
          t: 'richText',
          v: `<p>${faker.lorem.sentence()}</p>`,
        },
      },
      discounts: [$.randomCreateDiscountPayload()],
      applyType: 'coupon',
    };
  },
  randomUpdatePromotionPayload: discountIds => ({
    attributes: $.randomCreatePromotionPayload().attributes,
    discounts: discountIds.map(id =>
      Object.assign({}, $.randomCreateDiscountPayload(), { id })),
    applyType: 'coupon',
  }),
  randomCouponPayload: (promotionId) => {
    const now = Date.now();
    return {
      promotion: promotionId,
      attributes: {
        usageRules: {
          t: 'usageRules',
          v: {
            isExclusive: false,
            isUnlimitedPerCode: true,
            usesPerCode: 1,
            isUnlimitedPerCustomer: true,
            usesPerCustomer: 1,
          },
        },
        name: {
          t: 'string',
          v: `BVT-COUPON-${now}`,
        },
        storefrontName: {
          t: 'richText',
          v: `BVT-COUPON-${now}`,
        },
        description: {
          t: 'text',
          v: faker.lorem.sentence(),
        },
        details: {
          t: 'richText',
          v: `<p>${faker.lorem.sentence()}</p>`,
        },
        activeFrom: {
          t: 'datetime',
          v: '2017-01-24T22:03:58.698Z',
        },
        activeTo: {
          t: 'datetime',
          v: null,
        },
      },
    };
  },
  randomGenerateCouponCodesPayload: () => {
    const prefix = `bvt-${Date.now()}-`;
    const quantity = $.randomNumber(1, 40);
    const digits = Math.floor(Math.log10(quantity)) + 1;
    return {
      prefix,
      quantity,
      length: prefix.length + $.randomNumber(digits, digits + 10),
    };
  },
  randomStoreAdminPayload: () => ({
    name: faker.name.findName(),
    email: `${Date.now()}@bvt.com`,
    org: 'tenant',
  }),
  randomItemsUpdatePayload: (skuCodes, { minQuantity, maxQuantity } = {}) =>
    skuCodes.reduce((payload, skuCode) =>
      Object.assign({}, payload, {
        [skuCode]: {
          quantity: $.randomNumber(minQuantity || 1, maxQuantity || 10),
        },
      }),
      {},
    ),
  randomLineItemsPayload: (skuCodes, { minQuantity, maxQuantity } = {}) =>
    skuCodes.map(skuCode => ({
      sku: skuCode,
      quantity: $.randomNumber(minQuantity || 1, maxQuantity || 10),
    })),
};

export default $;
