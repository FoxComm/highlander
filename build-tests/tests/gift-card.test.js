import test from '../helpers/test';
import testNotes from './testNotes';
import Api from '../helpers/Api';
import isNumber from '../helpers/isNumber';
import isString from '../helpers/isString';
import isDate from '../helpers/isDate';
import $ from '../payloads';

test('Can create a gift card', async (t) => {
  const api = await Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const payload = $.randomGiftCardPayload();
  const newGiftCard = await api.giftCards.create(payload).then(r => r[0].giftCard);
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

test('Can view gift card details', async (t) => {
  const api = await Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const payload = $.randomGiftCardPayload();
  const newGiftCard = await api.giftCards.create(payload).then(r => r[0].giftCard);
  const foundGiftCard = await api.giftCards.one(newGiftCard.code);
  t.deepEqual(foundGiftCard, newGiftCard);
});

test('Can put a gift card "On Hold"', async (t) => {
  const api = await Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const newGiftCard = await api.giftCards.create($.randomGiftCardPayload()).then(r => r[0].giftCard);
  const updatedGiftCard = await api.giftCards.update(newGiftCard.code, { state: 'onHold' });
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

test('Can "Cancel" a gift card', async (t) => {
  const api = await Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const newGiftCard = await api.giftCards.create($.randomGiftCardPayload()).then(r => r[0].giftCard);
  const payload = { state: 'canceled', reasonId: 1 };
  const updatedGiftCard = await api.giftCards.update(newGiftCard.code, payload);
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

test('Can make gift card "Active"', async (t) => {
  const api = await Api.withCookies();
  await api.auth.login($.adminEmail, $.adminPassword, $.adminOrg);
  const newGiftCard = await api.giftCards.create($.randomGiftCardPayload()).then(r => r[0].giftCard);
  const updatedGiftCardOnHold = await api.giftCards.update(newGiftCard.code, { state: 'onHold' });
  t.is(updatedGiftCardOnHold.state, 'onHold');
  const updatedActiveGiftCard = await api.giftCards.update(newGiftCard.code, { state: 'active' });
  t.is(updatedActiveGiftCard.state, 'active');
});

testNotes({
  objectType: 'gift-card',
  createObject: api => api.giftCards.create($.randomGiftCardPayload()).then(r => r[0].giftCard),
  selectId: giftCard => giftCard.code,
});
