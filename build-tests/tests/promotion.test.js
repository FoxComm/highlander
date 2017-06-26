import test from '../helpers/test';
import testNotes from './testNotes';
import { AdminApi } from '../helpers/Api';
import isNumber from '../helpers/isNumber';
import isDate from '../helpers/isDate';
import $ from '../payloads';

test('Can create a promotion', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const payload = $.randomCreatePromotionPayload();
  const newPromotion = await adminApi.promotions.create('default', payload);
  t.truthy(isNumber(newPromotion.id));
  t.is(newPromotion.context.name, 'default');
  t.is(newPromotion.applyType, payload.applyType);
  t.truthy(newPromotion.attributes.activeFrom);
  t.is(newPromotion.attributes.activeFrom.t, 'date');
  t.truthy(isDate(newPromotion.attributes.activeFrom.v));
  for (const key of Object.keys(payload.attributes)) {
    t.deepEqual(newPromotion.attributes[key], payload.attributes[key]);
  }
  for (let i = 0; i < payload.discounts.length; i += 1) {
    const payloadDiscount = payload.discounts[i];
    const promotionDiscount = newPromotion.discounts[i];
    t.truthy(isNumber(promotionDiscount.id));
    t.deepEqual(promotionDiscount.attributes, payloadDiscount.attributes);
  }
});

test('Can view promotion details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newPromotion = await adminApi.promotions.create('default', $.randomCreatePromotionPayload());
  const foundPromotion = await adminApi.promotions.one('default', newPromotion.id);
  t.deepEqual(foundPromotion, newPromotion);
});

test('Can update promotion details', async (t) => {
  const adminApi = await AdminApi.loggedIn(t);
  const newPromotion = await adminApi.promotions.create('default', $.randomCreatePromotionPayload());
  const payload = $.randomUpdatePromotionPayload(newPromotion.discounts.map(d => d.id));
  const updatedPromotion = await adminApi.promotions.update('default', newPromotion.id, payload);
  t.is(updatedPromotion.id, newPromotion.id);
  t.is(updatedPromotion.context.name, 'default');
  t.is(updatedPromotion.applyType, payload.applyType);
  t.is(updatedPromotion.attributes.activeFrom.t, 'date');
  t.truthy(isDate(updatedPromotion.attributes.activeFrom.v));
  for (const key of Object.keys(payload.attributes)) {
    t.deepEqual(updatedPromotion.attributes[key], payload.attributes[key]);
  }
  for (let i = 0; i < payload.discounts.length; i += 1) {
    const payloadDiscount = payload.discounts[i];
    const promotionDiscount = updatedPromotion.discounts[i];
    t.truthy(isNumber(promotionDiscount.id));
    t.deepEqual(promotionDiscount.attributes, payloadDiscount.attributes);
  }
});

testNotes({
  objectType: 'promotion',
  createObject: adminApi => adminApi.promotions.create('default', $.randomCreatePromotionPayload()),
});
