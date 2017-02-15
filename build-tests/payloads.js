import faker from 'faker';

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
      albums: [],
    };
  },
};

export default $;
