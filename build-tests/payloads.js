import faker from 'faker';

export default {
  adminEmail: 'admin@admin.com',
  adminPassword: 'password',
  adminOrg: 'tenant',
  customerOrg: 'merchant',
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
