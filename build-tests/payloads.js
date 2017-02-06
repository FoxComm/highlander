import faker from 'faker';

export default {
  adminEmail: 'admin@admin.com',
  adminPassword: 'password',
  adminOrg: 'tenant',
  customerOrg: 'merchant',
  randomUserCredentials: () => ({
    email: faker.internet.email().toLowerCase(),
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
  predefinedCreateCreditCardFromTokenPayload: {
    token: 'tok_18t2CWAVdiXyWQ8c4PnlJZAJ',
    holderName: 'John Coe',
    lastFour: '4242',
    expMonth: 5,
    expYear: 2021,
    brand: 'Visa',
    addressIsNew: false,
    billingAddress: {
      id: 10,
      region: {
        id: 4173,
        countryId: 234,
        name: 'Utah',
      },
      name: 'Dewayne Dach',
      address1: '883 Cold Feather Pass',
      address2: 'Suite 236',
      city: 'Utah',
      zip: '04289',
      isDefault: true,
      phoneNumber: '6792233629',
      regionId: 4173,
      state: 'Utah',
      country: 'United States',
    },
  },
};
