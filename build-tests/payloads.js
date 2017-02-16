import path from 'path';
import faker from 'faker';
import testImageBase64 from './assets/image.base64';

const randomDigit = () => faker.random.number(9).toString();
const randomMonth = () => faker.random.number({ min: 1, max: 12 });
const randomYear = () => faker.random.number({ min: 2017, max: 2020 });

const randomCvv = () => '###'.replace(/#/g, randomDigit);

const $ = {
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
    expMonth: randomMonth(),
    expYear: randomYear(),
    cvv: 100 + faker.random.number(899),
    address: $.randomCreateAddressPayload(),
  }),
  randomCreditCardCreatePayload: () => ({
    holderName: faker.name.findName(),
    number: $.testCardNumber,
    cvv: randomCvv(),
    expMonth: randomMonth(),
    expYear: randomYear(),
    address: $.randomCreateAddressPayload(),
    isDefault: false,
  }),
  randomCreateManualStoreCreditPayload: () => ({
    amount: faker.random.number({ min: 1000, max: 10000 }),
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
  randomProductPayload: () => {
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
      skus: [$.randomSkuPayload()],
    };
  },
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
    const imageCount = faker.random.number({
      min: minImages || 0,
      max: maxImages || 3,
    });
    for (let i = 0; i < imageCount; i += 1) {
      images.push($.randomImagePayload());
    }
    return {
      name: `A-${Date.now()}`,
      images,
    };
  },
};

export default $;
