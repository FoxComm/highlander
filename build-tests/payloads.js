import faker from 'faker';
import CreditCardGenerator from 'creditcard-generator';

const SUPPORTED_CREDIT_CARD_SCHEMES = ['MasterCard', 'Amex', 'Discover', 'Diners', 'JCB', 'VISA'];

const randomDigit = () => faker.random.number(9).toString();
const randomMonth = () => faker.random.number({ min: 1, max: 12 });
const randomYear = () => faker.random.number({ min: 2017, max: 2020 });

const randomCardNumber = () => CreditCardGenerator.GenCC(
  faker.random.arrayElement(SUPPORTED_CREDIT_CARD_SCHEMES),
)[0];

const randomCvv = () => '###'.replace(/#/g, randomDigit);

const $ = {
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
  randomCreditCardDetailsPayload: customerId => ({
    customerId,
    cardNumber: randomCardNumber(),
    expMonth: randomMonth(),
    expYear: randomYear(),
    cvv: 100 + faker.random.number(899),
    address: $.randomCreateAddressPayload(),
  }),
  randomCreditCardCreatePayload: () => ({
    holderName: faker.name.findName(),
    number: randomCardNumber(),
    cvv: randomCvv(),
    expMonth: randomMonth(),
    expYear: randomYear(),
    address: $.randomCreateAddressPayload(),
    isDefault: false,
  }),
};

export default $;
