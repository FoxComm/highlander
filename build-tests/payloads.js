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
    zip: faker.address.zipCode(),
    isDefault: false,
    phoneNumber: faker.phone.phoneNumber('##########'),
  }),
};
