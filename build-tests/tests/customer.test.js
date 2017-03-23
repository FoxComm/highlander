import test from '../helpers/test';
import testNotes from './testNotes';
import Api from '../helpers/Api';
import $ from '../payloads';
import isDate from '../helpers/isDate';
import isArray from '../helpers/isArray';
import isNumber from '../helpers/isNumber';

test('Can create a new customer', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  t.is(newCustomer.name, credentials.name);
  t.is(newCustomer.email, credentials.email);
});

test('Can view customer details', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const foundCustomer = await api.customers.one(newCustomer.id);
  t.is(foundCustomer.name, credentials.name);
  t.is(foundCustomer.email, credentials.email);
});

test('Can update customer details', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const otherCredentials = $.randomUserCredentials();
  const updatedCustomer = await api.customers.update(newCustomer.id, otherCredentials);
  t.is(updatedCustomer.name, otherCredentials.name);
  t.is(updatedCustomer.email, otherCredentials.email);
});

test('Can list shipping addresses', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const addresses = await api.customerAddresses.list(newCustomer.id);
  t.truthy(isArray(addresses));
});

test('Can add a new shipping address', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const address = $.randomCreateAddressPayload();
  const addedAddress = await api.customerAddresses.add(newCustomer.id, address);
  t.is(addedAddress.region.id, address.regionId);
  t.is(addedAddress.name, address.name);
  t.is(addedAddress.address1, address.address1);
  t.is(addedAddress.address2, address.address2);
  t.is(addedAddress.city, address.city);
  t.is(addedAddress.zip, address.zip);
  t.is(addedAddress.phoneNumber, address.phoneNumber);
});

test('Can update shipping address details', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const address = $.randomCreateAddressPayload();
  const addedAddress = await api.customerAddresses.add(newCustomer.id, address);
  const otherAddress = $.randomCreateAddressPayload();
  const updatedAddress = await api.customerAddresses.update(newCustomer.id, addedAddress.id, otherAddress);
  t.is(updatedAddress.region.id, otherAddress.regionId);
  t.is(updatedAddress.name, otherAddress.name);
  t.is(updatedAddress.address1, otherAddress.address1);
  t.is(updatedAddress.address2, otherAddress.address2);
  t.is(updatedAddress.city, otherAddress.city);
  t.is(updatedAddress.zip, otherAddress.zip);
  t.is(updatedAddress.phoneNumber, otherAddress.phoneNumber);
  const foundAddress = await api.customerAddresses.one(newCustomer.id, addedAddress.id);
  t.is(foundAddress.region.id, otherAddress.regionId);
  t.is(foundAddress.name, otherAddress.name);
  t.is(foundAddress.address1, otherAddress.address1);
  t.is(foundAddress.address2, otherAddress.address2);
  t.is(foundAddress.city, otherAddress.city);
  t.is(foundAddress.zip, otherAddress.zip);
  t.is(foundAddress.phoneNumber, otherAddress.phoneNumber);
});

test('Can delete a shipping address', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const address = $.randomCreateAddressPayload();
  const addedAddress = await api.customerAddresses.add(newCustomer.id, address);
  await api.customerAddresses.delete(newCustomer.id, addedAddress.id);
  const foundAddress = await api.customerAddresses.one(newCustomer.id, addedAddress.id);
  t.truthy(foundAddress.deletedAt);
});

test('Can list customer\'s credit cards', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const creditCards = await api.customerCreditCards.list(newCustomer.id);
  t.truthy(isArray(creditCards));
});

test('Can add customer\'s credit card', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const creditCardDetails = $.randomCreditCardDetailsPayload(newCustomer.id);
  const newTokenResponse = await api.dev.creditCardToken(creditCardDetails);
  const payload = {
    token: newTokenResponse.token,
    lastFour: newTokenResponse.lastFour,
    expYear: creditCardDetails.expYear,
    expMonth: creditCardDetails.expMonth,
    brand: newTokenResponse.brand,
    holderName: creditCardDetails.address.name,
    billingAddress: creditCardDetails.address,
    addressIsNew: true,
  };
  const addedCard = await api.customerCreditCards.add(newCustomer.id, payload);
  t.truthy(addedCard.id);
  t.is(addedCard.customerId, newCustomer.id);
  t.is(addedCard.holderName, payload.holderName);
  t.is(addedCard.lastFour, payload.lastFour);
  t.is(addedCard.expMonth, payload.expMonth);
  t.is(addedCard.expYear, payload.expYear);
  t.is(addedCard.brand, payload.brand);
  t.is(addedCard.isDefault, false);
  t.is(addedCard.inWallet, true);
  t.is(addedCard.address.region.id, payload.billingAddress.regionId);
  t.is(addedCard.address.name, payload.billingAddress.name);
  t.is(addedCard.address.address1, payload.billingAddress.address1);
  t.is(addedCard.address.address2, payload.billingAddress.address2);
  t.is(addedCard.address.city, payload.billingAddress.city);
  t.is(addedCard.address.zip, payload.billingAddress.zip);
  t.is(addedCard.address.phoneNumber, payload.billingAddress.phoneNumber);
});

test('Can issue store credit', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const payload = $.randomStoreCreditPayload();
  const newStoreCredit = await api.customers.issueStoreCredit(newCustomer.id, payload);
  t.truthy(newStoreCredit.id);
  t.truthy(newStoreCredit.originId);
  t.is(newStoreCredit.originType, 'csrAppeasement');
  t.is(newStoreCredit.currency, 'USD');
  t.is(newStoreCredit.customerId, newCustomer.id);
  t.is(newStoreCredit.originalBalance, payload.amount);
  t.is(newStoreCredit.currentBalance, payload.amount);
  t.is(newStoreCredit.availableBalance, payload.amount);
  t.is(newStoreCredit.state, 'active');
  t.truthy(isDate(newStoreCredit.createdAt));
});

test('Can list customer groups', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const customerGroups = await api.customerGroups.list();
  t.truthy(isArray(customerGroups));
});

test('Can create a new customer group', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const payload = $.randomCustomerGroupPayload();
  const newCustomerGroup = await api.customerGroups.create(payload);
  t.truthy(isNumber(newCustomerGroup.id));
  t.truthy(isDate(newCustomerGroup.createdAt));
  t.is(newCustomerGroup.type, 'dynamic');
  t.is(newCustomerGroup.name, payload.name);
  t.deepEqual(newCustomerGroup.clientState, payload.clientState);
  t.deepEqual(newCustomerGroup.elasticRequest, payload.elasticRequest);
});

test('Can view customer group details', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const newCustomerGroup = await api.customerGroups.create($.randomCustomerGroupPayload());
  const foundCustomerGroup = await api.customerGroups.one(newCustomerGroup.id);
  t.deepEqual(foundCustomerGroup, newCustomerGroup);
});

test('Can update customer group details', async (t) => {
  const api = Api.withCookies(t);
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const newCustomerGroup = await api.customerGroups.create($.randomCustomerGroupPayload());
  const payload = $.randomCustomerGroupPayload();
  const updatedCustomerGroup = await api.customerGroups.update(newCustomerGroup.id, payload);
  t.is(updatedCustomerGroup.id, newCustomerGroup.id);
  t.is(updatedCustomerGroup.createdAt, newCustomerGroup.createdAt);
  t.is(updatedCustomerGroup.name, payload.name);
  t.deepEqual(updatedCustomerGroup.clientState, payload.clientState);
  t.deepEqual(updatedCustomerGroup.elasticRequest, payload.elasticRequest);
  t.not(updatedCustomerGroup.updatedAt, newCustomerGroup.updatedAt);
});

testNotes({
  objectType: 'customer',
  createObject: api => api.customers.create($.randomUserCredentials()),
});
