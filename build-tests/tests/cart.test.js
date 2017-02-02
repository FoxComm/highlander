import faker from 'faker';
import test from '../helpers/test';
import startRandomUserSession from '../helpers/startRandomUserSession';
import Api from '../helpers/Api';
import $ from '../payloads';

test('Can set shipping address', async (t) => {
  const api = Api.withCookies();
  await startRandomUserSession(api);
  await api.cart.get();
  const payload = $.randomCreateAddressPayload();
  const fullOrder1 = await api.cart.setShippingAddress(payload).then(r => r.result);
  const shippingAddress = fullOrder1.shippingAddress;
  t.is(shippingAddress.name, payload.name);
  t.is(shippingAddress.region.id, payload.regionId);
  t.is(shippingAddress.address1, payload.address1);
  t.is(shippingAddress.address2, payload.address2);
  t.is(shippingAddress.city, payload.city);
  t.is(shippingAddress.zip, payload.zip);
  t.is(shippingAddress.isDefault, payload.isDefault);
  t.is(shippingAddress.phoneNumber, payload.phoneNumber);
  const fullOrder2 = await api.cart.get();
  t.deepEqual(fullOrder1, fullOrder2);
});

test('Can view available shipping methods', async (t) => {
  const api = Api.withCookies();
  await startRandomUserSession(api);
  await api.cart.get();
  const shippingMethods = await api.cart.getShippingMethods();
  t.truthy(shippingMethods);
  t.is(shippingMethods.constructor.name, 'Array');
});

test('Can choose shipping method', async (t) => {
  const api = Api.withCookies();
  await startRandomUserSession(api);
  await api.cart.get();
  const createAddressPayload = $.randomCreateAddressPayload();
  await api.cart.setShippingAddress(createAddressPayload);
  const shippingMethods = await api.cart.getShippingMethods();
  const shippingMethod = faker.random.arrayElement(shippingMethods);
  const fullOrder = await api.cart.chooseShippingMethod(shippingMethod.id).then(r => r.result);
  t.deepEqual(fullOrder.shippingMethod, shippingMethod);
});
