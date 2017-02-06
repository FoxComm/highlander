import test from '../helpers/test';
import Api from '../helpers/Api';
import $ from '../payloads';
import stripObjects from '../helpers/stripObjects';

test('Can create a new customer', async (t) => {
  const api = Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  t.is(newCustomer.name, credentials.name);
  t.is(newCustomer.email, credentials.email);
});

test('Can view customer details', async (t) => {
  const api = Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const foundCustomer = await api.customers.one(newCustomer.id);
  t.is(foundCustomer.name, credentials.name);
  t.is(foundCustomer.email, credentials.email);
});

test('Can update customer details', async (t) => {
  const api = Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const otherCredentials = $.randomUserCredentials();
  const updatedCustomer = await api.customers.update(newCustomer.id, otherCredentials);
  t.is(updatedCustomer.name, otherCredentials.name);
  t.is(updatedCustomer.email, otherCredentials.email);
});

test('Can list shipping addresses', async (t) => {
  const api = Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const addresses = await api.customerAddresses.list(newCustomer.id);
  t.is(addresses.constructor.name, 'Array');
});

test('Can add a new shipping address', async (t) => {
  const api = Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const address = $.randomCreateAddressPayload();
  const addedAddress = await api.customerAddresses.add(newCustomer.id, address);
  t.is(addedAddress.region.id, address.regionId);
  const [strippedAddress, strippedAddedAddress] =
    stripObjects(['id', 'region', 'regionId'], address, addedAddress);
  t.deepEqual(strippedAddress, strippedAddedAddress);
});

test('Can update shipping address details', async (t) => {
  const api = Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const address = $.randomCreateAddressPayload();
  const addedAddress = await api.customerAddresses.add(newCustomer.id, address);
  const otherAddress = $.randomCreateAddressPayload();
  const updatedAddress = await api.customerAddresses.update(newCustomer.id, addedAddress.id, otherAddress);
  const foundAddress = await api.customerAddresses.one(newCustomer.id, addedAddress.id);
  const [strippedOtherAddress, strippedUpdatedAddress, strippedFoundAddress] =
    stripObjects(['id', 'region', 'regionId'], otherAddress, updatedAddress, foundAddress);
  t.deepEqual(strippedUpdatedAddress, strippedOtherAddress);
  t.deepEqual(strippedFoundAddress, strippedOtherAddress);
});

test('Can delete a shipping address', async (t) => {
  const api = Api.withCookies();
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
  const api = Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  const creditCards = await api.customerCreditCards.list(newCustomer.id);
  t.is(creditCards.constructor.name, 'Array');
});

test('Can add customer\'s credit card', async () => {
  const api = Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await api.customers.create(credentials);
  await api.customerCreditCards.add(newCustomer.id, $.predefinedCreateCreditCardFromTokenPayload);
});
