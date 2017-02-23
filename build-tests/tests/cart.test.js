import test from '../helpers/test';
import startRandomUserSession from '../helpers/startRandomUserSession';
import createCreditCard from '../helpers/createCreditCard';
import Api from '../helpers/Api';
import isArray from '../helpers/isArray';
import isString from '../helpers/isString';
import isNumber from '../helpers/isNumber';
import $ from '../payloads';

test('Can add line item', async (t) => {
  const adminApi = Api.withCookies();
  await adminApi.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
  const newProduct = await adminApi.products.create('default', productPayload);
  const customerApi = Api.withCookies();
  await startRandomUserSession(customerApi);
  await customerApi.cart.get();
  const skuCode = newProduct.skus[0].attributes.code.v;
  const quantity = $.randomNumber(1, 10);
  const fullOrder = await customerApi.cart.addSku(skuCode, quantity).then(r => r.result);
  t.truthy(fullOrder.lineItems);
  t.truthy(isArray(fullOrder.lineItems.skus));
  t.is(fullOrder.lineItems.skus.length, 1);
  t.is(fullOrder.lineItems.skus[0].sku, skuCode);
  t.is(fullOrder.lineItems.skus[0].quantity, quantity);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrder);
});

test('Can update line item', async (t) => {
  const adminApi = Api.withCookies();
  await adminApi.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
  const newProduct = await adminApi.products.create('default', productPayload);
  const customerApi = Api.withCookies();
  await startRandomUserSession(customerApi);
  await customerApi.cart.get();
  const skuCode = newProduct.skus[0].attributes.code.v;
  await customerApi.cart.addSku(skuCode, 1);
  const newQuantity = $.randomNumber(2, 10);
  const fullOrder = await customerApi.cart.updateQty(skuCode, newQuantity).then(r => r.result);
  t.truthy(fullOrder.lineItems);
  t.truthy(isArray(fullOrder.lineItems.skus));
  t.is(fullOrder.lineItems.skus.length, 1);
  t.is(fullOrder.lineItems.skus[0].sku, skuCode);
  t.is(fullOrder.lineItems.skus[0].quantity, newQuantity);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrder);
});

test('Can remove line item', async (t) => {
  const adminApi = Api.withCookies();
  await adminApi.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const productPayload = $.randomProductPayload({ minSkus: 1, maxSkus: 1 });
  const newProduct = await adminApi.products.create('default', productPayload);
  const customerApi = Api.withCookies();
  await startRandomUserSession(customerApi);
  await customerApi.cart.get();
  const skuCode = newProduct.skus[0].attributes.code.v;
  await customerApi.cart.addSku(skuCode, 1);
  const fullOrder = await customerApi.cart.removeSku(skuCode).then(r => r.result);
  t.truthy(fullOrder.lineItems);
  t.truthy(isArray(fullOrder.lineItems.skus));
  t.is(fullOrder.lineItems.skus.length, 0);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrder);
});

test('Can set shipping address', async (t) => {
  const api = Api.withCookies();
  await startRandomUserSession(api);
  await api.cart.get();
  const payload = $.randomCreateAddressPayload();
  const fullOrder = await api.cart.setShippingAddress(payload).then(r => r.result);
  const shippingAddress = fullOrder.shippingAddress;
  t.is(shippingAddress.name, payload.name);
  t.is(shippingAddress.region.id, payload.regionId);
  t.is(shippingAddress.address1, payload.address1);
  t.is(shippingAddress.address2, payload.address2);
  t.is(shippingAddress.city, payload.city);
  t.is(shippingAddress.zip, payload.zip);
  t.is(shippingAddress.isDefault, payload.isDefault);
  t.is(shippingAddress.phoneNumber, payload.phoneNumber);
  const foundOrder = await api.cart.get();
  t.deepEqual(foundOrder, fullOrder);
});

test('Can view available shipping methods', async (t) => {
  const api = Api.withCookies();
  await startRandomUserSession(api);
  await api.cart.get();
  await api.cart.setShippingAddress($.randomCreateAddressPayload());
  const shippingMethods = await api.cart.getShippingMethods();
  t.truthy(isArray(shippingMethods));
  for (const shippingMethod of shippingMethods) {
    t.truthy(isNumber(shippingMethod.id));
    t.truthy(isNumber(shippingMethod.price));
    t.truthy(isString(shippingMethod.name));
    t.truthy(isString(shippingMethod.code));
  }
});

test('Can choose shipping method', async (t) => {
  const api = Api.withCookies();
  await startRandomUserSession(api);
  await api.cart.get();
  await api.cart.setShippingAddress($.randomCreateAddressPayload());
  const shippingMethods = await api.cart.getShippingMethods();
  const shippingMethod = $.randomArrayElement(shippingMethods);
  const fullOrder = await api.cart.chooseShippingMethod(shippingMethod.id).then(r => r.result);
  t.deepEqual(fullOrder.shippingMethod, shippingMethod);
  const foundOrder = await api.cart.get();
  t.deepEqual(foundOrder, fullOrder);
});

test('Can apply a credit card', async (t) => {
  const adminApi = Api.withCookies();
  await adminApi.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const credentials = $.randomUserCredentials();
  const newCustomer = await adminApi.customers.create(credentials);
  const newCard = await createCreditCard(adminApi, newCustomer.id);
  const customerApi = Api.withCookies();
  await customerApi.auth.login(credentials.email, credentials.password, $.customerOrg);
  await customerApi.cart.get();
  const fullOrder = await customerApi.cart.addCreditCard(newCard.id).then(r => r.result);
  t.truthy(isArray(fullOrder.paymentMethods));
  t.truthy(fullOrder.paymentMethods.length, 1);
  const orderCreditCard = fullOrder.paymentMethods[0];
  t.is(orderCreditCard.type, 'creditCard');
  t.is(orderCreditCard.id, newCard.id);
  t.is(orderCreditCard.customerId, newCard.customerId);
  t.is(orderCreditCard.holderName, newCard.holderName);
  t.is(orderCreditCard.lastFour, newCard.lastFour);
  t.is(orderCreditCard.expMonth, newCard.expMonth);
  t.is(orderCreditCard.expYear, newCard.expYear);
  t.is(orderCreditCard.brand, newCard.brand);
  t.deepEqual(orderCreditCard.address, newCard.address);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrder);
});

test('Can apply a gift card', async (t) => {
  const adminApi = Api.withCookies();
  await adminApi.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const giftCardPayload = $.randomGiftCardPayload();
  const newGiftCard = await adminApi.giftCards.create(giftCardPayload).then(r => r[0].giftCard);
  const customerApi = Api.withCookies();
  await startRandomUserSession(customerApi);
  await customerApi.cart.get();
  const payload = {
    code: newGiftCard.code,
    amount: newGiftCard.availableBalance,
  };
  const fullOrder = await customerApi.cart.addGiftCard(payload).then(r => r.result);
  t.truthy(isArray(fullOrder.paymentMethods));
  t.truthy(fullOrder.paymentMethods.length, 1);
  const orderGiftCard = fullOrder.paymentMethods[0];
  t.is(orderGiftCard.code, payload.code);
  t.is(orderGiftCard.amount, payload.amount);
  t.is(orderGiftCard.type, 'giftCard');
  t.is(orderGiftCard.currentBalance, newGiftCard.currentBalance);
  t.is(orderGiftCard.availableBalance, newGiftCard.availableBalance);
  t.is(orderGiftCard.createdAt, newGiftCard.createdAt);
  const foundOrder = await customerApi.cart.get();
  t.deepEqual(foundOrder, fullOrder);
});
