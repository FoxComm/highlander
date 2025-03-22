import test from '../../helpers/test';
import testNotes from './testNotes';
import { AdminApi, CustomerApi } from '../../helpers/Api';
import createCreditCard from '../../helpers/createCreditCard';
import waitFor from '../../helpers/waitFor';
import isNumber from '../../helpers/isNumber';
import isString from '../../helpers/isString';
import isDate from '../../helpers/isDate';
import $ from '../../payloads';
import config from '../../config';

test('[bvt] Can create a gift card', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const payload = $.randomGiftCardPayload();
  const newGiftCard = await adminApi.giftCards.create(payload);
  t.truthy(isNumber(newGiftCard.id));
  t.truthy(isDate(newGiftCard.createdAt));
  t.truthy(isString(newGiftCard.code));
  t.is(newGiftCard.code.length, 16);
  t.truthy(isNumber(newGiftCard.originId));
  t.is(newGiftCard.originType, 'csrAppeasement');
  t.is(newGiftCard.state, 'active');
  t.is(newGiftCard.currency, 'USD');
  t.is(newGiftCard.originalBalance, payload.balance);
  t.is(newGiftCard.availableBalance, payload.balance);
  t.is(newGiftCard.currentBalance, payload.balance);
  t.is(newGiftCard.storeAdmin.email, $.adminEmail);
  t.is(newGiftCard.storeAdmin.name, $.adminName);
});

test('[bvt] Can view gift card details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const payload = $.randomGiftCardPayload();
  const newGiftCard = await adminApi.giftCards.create(payload);
  const foundGiftCard = await adminApi.giftCards.one(newGiftCard.code);
  t.deepEqual(foundGiftCard, newGiftCard);
});

test('[bvt] Can put a gift card "On Hold"', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newGiftCard = await adminApi.giftCards.create($.randomGiftCardPayload());
  const updatedGiftCard = await adminApi.giftCards.update(newGiftCard.code, { state: 'onHold' });
  t.is(updatedGiftCard.state, 'onHold');
  t.is(updatedGiftCard.id, newGiftCard.id);
  t.is(updatedGiftCard.createdAt, newGiftCard.createdAt);
  t.is(updatedGiftCard.code, newGiftCard.code);
  t.is(updatedGiftCard.originId, newGiftCard.originId);
  t.is(updatedGiftCard.originType, newGiftCard.originType);
  t.is(updatedGiftCard.currency, newGiftCard.currency);
  t.is(updatedGiftCard.originalBalance, newGiftCard.originalBalance);
  t.is(updatedGiftCard.availableBalance, newGiftCard.availableBalance);
  t.is(updatedGiftCard.currentBalance, newGiftCard.currentBalance);
});

test('[bvt] Can "Cancel" a gift card', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newGiftCard = await adminApi.giftCards.create($.randomGiftCardPayload());
  const payload = { state: 'canceled', reasonId: 1 };
  const updatedGiftCard = await adminApi.giftCards.update(newGiftCard.code, payload);
  t.is(updatedGiftCard.state, payload.state);
  t.is(updatedGiftCard.canceledReason, payload.reasonId);
  t.is(updatedGiftCard.id, newGiftCard.id);
  t.is(updatedGiftCard.createdAt, newGiftCard.createdAt);
  t.is(updatedGiftCard.code, newGiftCard.code);
  t.is(updatedGiftCard.originId, newGiftCard.originId);
  t.is(updatedGiftCard.originType, newGiftCard.originType);
  t.is(updatedGiftCard.currency, newGiftCard.currency);
  t.is(updatedGiftCard.originalBalance, newGiftCard.originalBalance);
  t.is(updatedGiftCard.availableBalance, newGiftCard.availableBalance);
  t.is(updatedGiftCard.currentBalance, newGiftCard.currentBalance);
  t.is(updatedGiftCard.canceledAmount, newGiftCard.currentBalance);
});

test('[bvt] Can make gift card "Active"', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newGiftCard = await adminApi.giftCards.create($.randomGiftCardPayload());
  const updatedGiftCardOnHold = await adminApi.giftCards.update(newGiftCard.code, { state: 'onHold' });
  t.is(updatedGiftCardOnHold.state, 'onHold');
  const updatedActiveGiftCard = await adminApi.giftCards.update(newGiftCard.code, { state: 'active' });
  t.is(updatedActiveGiftCard.state, 'active');
});

if (config.testGiftCardFlow) {
  test('[bvt] Can send gift card to a customer', async (t) => {
    const adminApi = await AdminApi.loggedIn(t);
    const credentials = $.randomUserCredentials();
    const newCustomer = await adminApi.customers.create(credentials);
    const newCard = await createCreditCard(adminApi, newCustomer.id);
    const inventory = await adminApi.inventories.get($.testGiftCardSkuCode);
    const stockItemId = inventory.summary.find(item => item.type === 'Sellable').stockItem.id;
    await adminApi.inventories.increment(stockItemId, { qty: 1, status: 'onHand', type: 'Sellable' });
    const customerApi = new CustomerApi(t);
    await customerApi.auth.login(credentials.email, credentials.password, $.customerOrg);
    await customerApi.cart.get();
    const giftCardAttributes = $.randomGiftCardAttributes({ senderName: credentials.name });
    await customerApi.cart.addSku($.testGiftCardSkuCode, 1, giftCardAttributes);
    await customerApi.cart.setShippingAddress($.randomCreateAddressPayload());
    const shippingMethod = $.randomArrayElement(await customerApi.cart.getShippingMethods());
    await customerApi.cart.chooseShippingMethod(shippingMethod.id);
    await customerApi.cart.addCreditCard(newCard.id);
    const fullOrder = await customerApi.cart.checkout();
    await adminApi.orders.update(fullOrder.referenceNumber, { state: 'fulfillmentStarted' });
    await adminApi.orders.update(fullOrder.referenceNumber, { state: 'shipped' });
    const newGiftCardCode = await waitFor(500, 10000, () =>
      adminApi.orders.one(fullOrder.referenceNumber)
        .then(r => r.result.lineItems.skus[0].attributes.giftCard.code),
      isString);
    const foundGiftCard = await adminApi.giftCards.one(newGiftCardCode);
    t.is(foundGiftCard.message, giftCardAttributes.giftCard.message);
    t.is(foundGiftCard.recipientName, giftCardAttributes.giftCard.recipientName);
    t.is(foundGiftCard.recipientEmail, giftCardAttributes.giftCard.recipientEmail);
    t.is(foundGiftCard.senderName, giftCardAttributes.giftCard.senderName);
  });
}

testNotes({
  objectType: 'gift-card',
  createObject: adminApi => adminApi.giftCards.create($.randomGiftCardPayload()),
  selectId: giftCard => giftCard.code,
});
